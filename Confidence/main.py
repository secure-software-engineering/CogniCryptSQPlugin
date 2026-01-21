from flask import Flask, request, jsonify
from flask_cors import CORS
from gcnModel import calculating_confidence

app = Flask(__name__)
CORS(app)
@app.route('/fp', methods=['POST'])
def entry_point():
    request_data = request.get_json()
    hashcode = request_data.get("hashcode")
    dot_graph = request_data.get("dot_graph")

    result = calculating_confidence(hashcode, dot_graph)
    return jsonify(result)


if __name__ == '__main__':
    app.run(host='0.0.0.0', port=8001)