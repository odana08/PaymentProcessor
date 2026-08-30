#!/usr/bin/env bash

set -Eeuo pipefail

API_BASE_URL="${API_BASE_URL:-http://localhost:8080}"
PAYMENTS_URL="${API_BASE_URL%/}/api/payments"
CURL_CONNECT_TIMEOUT="${CURL_CONNECT_TIMEOUT:-5}"
CURL_MAX_TIME="${CURL_MAX_TIME:-30}"

for required_command in curl python3; do
    if ! command -v "$required_command" >/dev/null 2>&1; then
        printf 'Required command is not installed: %s\n' "$required_command" >&2
        exit 1
    fi
done

TEMP_DIRECTORY="$(mktemp -d)"
RESPONSE_BODY="$TEMP_DIRECTORY/response-body.json"
CREATED_REFERENCE=""
CURRENT_CHECK="initialization"

cleanup() {
    local exit_code=$?
    trap - EXIT
    set +e

    if [[ -n "$CREATED_REFERENCE" ]]; then
        curl --silent --show-error \
            --connect-timeout "$CURL_CONNECT_TIMEOUT" \
            --max-time "$CURL_MAX_TIME" \
            --request DELETE \
            "$PAYMENTS_URL/$CREATED_REFERENCE" \
            >/dev/null 2>&1
    fi

    if [[ -n "$TEMP_DIRECTORY" && -d "$TEMP_DIRECTORY" ]]; then
        rm -rf -- "$TEMP_DIRECTORY"
    fi

    exit "$exit_code"
}

trap cleanup EXIT

fail() {
    local message=$1
    printf '\nFAIL [%s]: %s\n' "$CURRENT_CHECK" "$message" >&2
    if [[ -s "$RESPONSE_BODY" ]]; then
        printf 'Response body:\n' >&2
        python3 -m json.tool "$RESPONSE_BODY" >&2 2>/dev/null || sed -n '1,120p' "$RESPONSE_BODY" >&2
    fi
    exit 1
}

pass() {
    printf 'PASS: %s\n' "$1"
}

request() {
    local method=$1
    local url=$2
    local expected_status=$3
    local payload=${4-}
    local actual_status
    local curl_arguments=(
        --silent
        --show-error
        --connect-timeout "$CURL_CONNECT_TIMEOUT"
        --max-time "$CURL_MAX_TIME"
        --request "$method"
        --header 'Accept: application/json'
        --output "$RESPONSE_BODY"
        --write-out '%{http_code}'
    )

    CURRENT_CHECK="$method $url"
    : >"$RESPONSE_BODY"

    if [[ -n "$payload" ]]; then
        curl_arguments+=(--header 'Content-Type: application/json' --data "$payload")
    fi

    if ! actual_status="$(curl "${curl_arguments[@]}" "$url")"; then
        fail "curl could not reach the API. Start Spring Boot and verify API_BASE_URL=$API_BASE_URL"
    fi

    if [[ "$actual_status" != "$expected_status" ]]; then
        fail "expected HTTP $expected_status but received HTTP $actual_status"
    fi
}

assert_json() {
    local expression=$1
    local description=$2

    if ! python3 - "$RESPONSE_BODY" "$expression" <<'PY'
import json
import sys
from decimal import Decimal

body_path, expression = sys.argv[1:]
try:
    with open(body_path, encoding="utf-8") as body_file:
        data = json.load(body_file, parse_float=Decimal, parse_int=Decimal)
except (OSError, json.JSONDecodeError) as error:
    print(f"Could not read JSON response: {error}", file=sys.stderr)
    raise SystemExit(1)

allowed_names = {
    "data": data,
    "Decimal": Decimal,
    "all": all,
    "any": any,
    "len": len,
    "set": set,
}

try:
    matched = bool(eval(expression, {"__builtins__": {}, **allowed_names}, {}))
except Exception as error:
    print(f"JSON assertion could not be evaluated: {error}", file=sys.stderr)
    raise SystemExit(1)

raise SystemExit(0 if matched else 1)
PY
    then
        fail "$description"
    fi
}

read_created_reference() {
    python3 - "$RESPONSE_BODY" <<'PY'
import json
import sys
import uuid

with open(sys.argv[1], encoding="utf-8") as body_file:
    data = json.load(body_file)

reference = data.get("reference")
if not isinstance(reference, str):
    raise SystemExit("Create response does not contain a string reference")

uuid.UUID(reference)
print(reference)
PY
}

printf 'Testing Fee Calculator API at %s\n\n' "$API_BASE_URL"

request GET "$PAYMENTS_URL/options" 200
assert_json 'set(data["paymentTypes"]) >= {"DOMESTIC_FEE", "INTERNATIONAL_FEE", "CHEQUE_FEE"}' \
    'options response does not contain all supported payment types'
assert_json 'set(data["notificationChannels"]) >= {"EMAIL", "SMS"}' \
    'options response does not contain all supported notification channels'
pass 'payment options'

create_payload='{
  "paymentType": "DOMESTIC_FEE",
  "amountInCents": 10000,
  "currency": "JOD",
  "notificationChannels": ["EMAIL"]
}'
request POST "$PAYMENTS_URL" 201 "$create_payload"
CREATED_REFERENCE="$(read_created_reference)" || fail 'create response contains an invalid reference'
assert_json 'data["reference"] == "'"$CREATED_REFERENCE"'"' 'create response reference does not match'
assert_json 'data["paymentType"] == "DOMESTIC_FEE"' 'create response has the wrong payment type'
assert_json 'data["amountInCents"] == Decimal("10000")' 'create response has the wrong amount'
assert_json 'data["currency"] == "JOD"' 'create response has the wrong currency'
assert_json 'data["notificationChannels"] == ["EMAIL"]' 'create response has the wrong channels'
assert_json 'data["feeInCents"] == Decimal("150")' 'domestic fee should be 150 cents'
assert_json 'data["totalInCents"] == Decimal("10150")' 'create response has the wrong total'
assert_json 'data["status"] == "PROCESSED"' 'created payment should be processed'
pass "create payment ($CREATED_REFERENCE)"

request GET "$PAYMENTS_URL/$CREATED_REFERENCE" 200
assert_json 'data["reference"] == "'"$CREATED_REFERENCE"'"' 'GET by reference returned another payment'
assert_json 'data["status"] == "PROCESSED"' 'stored payment should be processed'
pass 'get payment by reference'

filtered_url="$PAYMENTS_URL?status=PROCESSED&currency=JOD&paymentType=domestic_fee&page=0&size=100&sort=reference,asc"
request GET "$filtered_url" 200
assert_json 'data["page"] == Decimal("0") and data["size"] == Decimal("100")' \
    'filtered collection returned incorrect paging metadata'
assert_json 'data["totalElements"] >= Decimal("1") and len(data["content"]) >= 1' \
    'filtered collection should contain at least the created payment'
assert_json 'all(item["status"] == "PROCESSED" and item["currency"] == "JOD" and item["paymentType"].lower() == "domestic_fee" for item in data["content"])' \
    'collection filters returned a nonmatching payment'
pass 'collection filters'

page_url="$PAYMENTS_URL?page=0&size=1&sort=reference,asc"
request GET "$page_url" 200
assert_json 'data["page"] == Decimal("0") and data["size"] == Decimal("1")' \
    'pagination metadata is incorrect'
assert_json 'data["totalElements"] >= Decimal("1") and len(data["content"]) == 1' \
    'page size was not applied in the database query'
pass 'pagination and sorting request'

replace_payload='{
  "paymentType": "CHEQUE_FEE",
  "amountInCents": 25000,
  "currency": "USD",
  "notificationChannels": ["SMS"]
}'
request PUT "$PAYMENTS_URL/$CREATED_REFERENCE" 200 "$replace_payload"
assert_json 'data["reference"] == "'"$CREATED_REFERENCE"'"' 'PUT changed the payment reference'
assert_json 'data["paymentType"] == "CHEQUE_FEE" and data["amountInCents"] == Decimal("25000")' \
    'PUT did not replace payment details'
assert_json 'data["currency"] == "USD" and data["notificationChannels"] == ["SMS"]' \
    'PUT did not replace currency and channels'
assert_json 'data["feeInCents"] == Decimal("500") and data["totalInCents"] == Decimal("25500")' \
    'PUT did not recalculate the cheque fee and total'
pass 'replace payment with PUT'

patch_payload='{"amountInCents": 50001}'
request PATCH "$PAYMENTS_URL/$CREATED_REFERENCE" 200 "$patch_payload"
assert_json 'data["paymentType"] == "CHEQUE_FEE" and data["amountInCents"] == Decimal("50001")' \
    'PATCH did not change only the requested amount'
assert_json 'data["currency"] == "USD" and data["notificationChannels"] == ["SMS"]' \
    'PATCH unexpectedly changed omitted fields'
assert_json 'data["feeInCents"] == Decimal("1000") and data["totalInCents"] == Decimal("51001")' \
    'PATCH did not recalculate the cheque fee and total'
pass 'partial update with PATCH'

request PATCH "$PAYMENTS_URL/$CREATED_REFERENCE" 400 '{}'
assert_json 'data["status"] == Decimal("400") and data["code"] == "INVALID_REQUEST"' \
    'empty PATCH should return the controlled invalid-request response'
pass 'empty PATCH validation error'

invalid_payload='{
  "paymentType": "",
  "amountInCents": 0,
  "currency": "JOD",
  "notificationChannels": []
}'
request POST "$PAYMENTS_URL" 400 "$invalid_payload"
assert_json 'data["status"] == Decimal("400") and data["error"] == "Bad Request"' \
    'invalid POST did not return the default 400 response'
assert_json 'data["code"] == "VALIDATION_FAILED" and len(data["violations"]) == 3' \
    'invalid POST did not return field validation details'
pass 'request validation failure'

duplicate_payload='{
  "paymentType": "DOMESTIC_FEE",
  "amountInCents": 10000,
  "currency": "JOD",
  "notificationChannels": ["EMAIL", "EMAIL"]
}'
request POST "$PAYMENTS_URL" 400 "$duplicate_payload"
assert_json 'data["status"] == Decimal("400") and data["code"] == "INVALID_REQUEST"' \
    'duplicate channels did not return the controlled invalid-request response'
pass 'duplicate notification-channel error'

missing_reference='00000000-0000-0000-0000-000000000000'
request GET "$PAYMENTS_URL/$missing_reference" 404
assert_json 'data["status"] == Decimal("404") and data["code"] == "PAYMENT_NOT_FOUND"' \
    'missing payment did not return the controlled not-found response'
pass 'missing payment error'

deleted_reference="$CREATED_REFERENCE"
request DELETE "$PAYMENTS_URL/$deleted_reference" 204
CREATED_REFERENCE=""
if [[ -s "$RESPONSE_BODY" ]]; then
    fail 'DELETE 204 response should not contain a body'
fi
pass 'delete payment'

request GET "$PAYMENTS_URL/$deleted_reference" 404
assert_json 'data["status"] == Decimal("404") and data["code"] == "PAYMENT_NOT_FOUND"' \
    'deleted payment did not use the default missing-payment response'
pass 'verify deleted payment is missing'

printf '\nAll Fee Calculator API checks passed.\n'
