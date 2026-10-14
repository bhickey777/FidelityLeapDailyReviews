
from flask import Flask, request, jsonify
from flask_cors import CORS
from datetime import datetime, timezone
import uuid

app = Flask(__name__)

# Allow requests from Angular running on port 4200
CORS(app, resources={
    r"/accounts/*": {
        "origins": ["http://localhost:4200"]
    }
})

# Mock POST endpoint
@app.route('/accounts/<int:account_id>/orders', methods=['POST'])
def create_order(account_id):

    # Read Order DTO from request
    order = request.get_json(silent=True)

    if not isinstance(order, dict):
        return jsonify({"error": "Expected a JSON object"}), 400

    print(f"Received order for account {account_id}: {order}")

    # Generate a unique order ID
    order_id = str(uuid.uuid4())

    # Build mock response
    response = {
        "orderId": order_id,
        "accountId": account_id,
        "ticker": order.get("ticker"),
        "instrumentType": order.get("instrumentType"),
        "quantity": order.get("quantity"),
        "price": order.get("price"),
        "side": order.get("side"),
        "status": "CREATED",
        "createdAt": datetime.now(timezone.utc).isoformat()
    }

    return jsonify(response), 201


if __name__ == '__main__':
    app.run(host='localhost', port=8090, debug=True)

