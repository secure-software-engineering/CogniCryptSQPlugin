export default {
    getJSON: async (url) => {
      console.log(`[Mock] GET: ${url}`);
      return {};
    },
    post: async (url, data) => {
      console.log(`[Mock] POST: ${url}`, data);
      return {};
    },
  };
  