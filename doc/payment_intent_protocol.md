# QuickStartKS Payment Intent Protocol

Other APKs can start QuickStartKS with a JSON payment request. The request links the amount and order data to the home screen, transaction screens, result screen, and printed receipt.

## Intent action

```java
Intent intent = new Intent("com.kozen.quickstartks.action.PAY");
intent.setPackage("com.kozen.quickstartks_new");
intent.putExtra("payment_json", json);
startActivity(intent);
```

## Deeplink

```text
quickstartks://pay?payment_json={url-encoded-json}
```

## JSON fields

```json
{
  "requestId": "REQ-20260603-001",
  "amount": "24.80",
  "currency": "USD",
  "orderNo": "ORDER-20260603-001",
  "orderTime": "2026-06-03 15:30:00",
  "orderInfo": "Latte x2, Croissant x1",
  "paymentMethod": "QR_DISPLAY",
  "callbackPackage": "com.thirdparty.pos",
  "callbackAction": "com.thirdparty.pos.PAYMENT_RESULT",
  "callbackUri": "thirdpartypos://payment/result",
  "returnReceiptData": true
}
```

Supported aliases:

- `amount`: decimal amount string or number, normalized to two decimals.
- `amountCents`: integer cents fallback when `amount` is absent.
- `currency`: `USD` or `EUR`; defaults to USD when unknown.
- `orderNo`: aliases `orderId`, `orderNumber`.
- `orderTime`: aliases `createdAt`, `time`.
- `orderInfo`: aliases `description`, `summary`.
- `items`: optional array fallback for `orderInfo`, using `name`/`title` and `qty`/`quantity`.
- `paymentMethod`: optional startup method. Supported values are `CARD`, `QR_SCAN`, and `QR_DISPLAY`.
- `requestId`: optional caller request id; echoed in result.
- `callbackPackage`: optional target package for explicit broadcast callback.
- `callbackAction`: optional broadcast action for payment result.
- `callbackUri`: optional deeplink callback; receives `payment_result_json` as query parameter.
- `returnReceiptData`: optional boolean; defaults to `true`.

## Result callback

Preferred callback is an explicit broadcast:

```java
IntentFilter filter = new IntentFilter("com.thirdparty.pos.PAYMENT_RESULT");
// Register a receiver, or declare an exported receiver in AndroidManifest.xml.
```

QuickStartKS sends:

```java
Intent callback = new Intent(callbackAction);
callback.setPackage(callbackPackage);
callback.putExtra("payment_result_json", resultJson);
sendBroadcast(callback);
```

If `callbackUri` is provided, QuickStartKS also opens:

```text
{callbackUri}?payment_result_json={url-encoded-result-json}
```

`setResult()` is also populated for direct Activity-result integrations, but for cross-APK payment flows the broadcast/deeplink callback is the stable contract.

## Result JSON

Success example:

```json
{
  "requestId": "REQ-20260603-001",
  "status": "SUCCESS",
  "paymentMethod": "QR",
  "amount": "24.80",
  "currency": "USD",
  "orderNo": "ORDER-20260603-001",
  "orderTime": "2026-06-03 15:30:00",
  "orderInfo": "Latte x2, Croissant x1",
  "receipt": {
    "merchantName": "COMMERCE 1",
    "mid": "123645678",
    "tid": "1234",
    "operatorNo": "01",
    "transType": "QR SALE",
    "orderNo": "ORDER-20260603-001",
    "orderInfo": "Latte x2, Croissant x1",
    "batchNo": "000001",
    "voucherNo": "370873",
    "refNo": "260603153000",
    "authCode": "123456",
    "dateTime": "2026-06-03 15:30:00",
    "amount": "$24.80"
  }
}
```

Possible `status` values:

- `SUCCESS`
- `FAILED`
- `TIMEOUT`
- `CANCELLED`

For card payments, `receipt` may also include masked `cardNo`, `cardBrand`, `expiry`, and `cardHolder`. QR payments do not return card fields.

## Exception and cancel flows

All terminal states should callback once when callback fields are provided.

- User cancels card transaction by pressing the cancel button or ESC: `status=CANCELLED`, `paymentMethod=CARD`, no `receipt`.
- EMV kernel reports user cancel: `status=CANCELLED`, `paymentMethod=CARD`, no `receipt`.
- User cancels QR scan/camera screen: `status=CANCELLED`, `paymentMethod=QR`, no `receipt`.
- User backs out of QR display payment screen: `status=CANCELLED`, `paymentMethod=QR`, no `receipt`.
- Transaction start or processing fails: `status=FAILED`; if receipt data was not generated, `receipt` may be absent.
- Transaction timeout: `status=TIMEOUT`; if receipt data was not generated, `receipt` may be absent.

Cancelled result example:

```json
{
  "requestId": "REQ-20260603-001",
  "status": "CANCELLED",
  "paymentMethod": "CARD",
  "amount": "24.80",
  "currency": "USD",
  "orderNo": "ORDER-20260603-001",
  "orderTime": "2026-06-03 15:30:00",
  "orderInfo": "Latte x2, Croissant x1",
  "errorMessage": "Cancel payment"
}
```

## Receipt field rules

Bank card receipt:

- Prints merchant, terminal, operator, order, card number, card type, expiry, batch, voucher, ref, auth, customer, date/time, amount, and cardholder signature.

QR receipt:

- Prints merchant, terminal, operator, order, batch, voucher, ref, auth, date/time, and amount.
- Does not print card number, card type, expiry date, customer/cardholder, or cardholder signature.
