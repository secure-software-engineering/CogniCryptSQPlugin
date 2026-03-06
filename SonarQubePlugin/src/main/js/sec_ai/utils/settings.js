import sonarRequest from "sonar-request";

const response = await sonarRequest.getJSON("/api/secai/getAIConfiguration");
console.log(response)

export const SERVER_IP = response.flask_ip;
export const OPENAI = response.openai;
export const GOOGLE = response.google;