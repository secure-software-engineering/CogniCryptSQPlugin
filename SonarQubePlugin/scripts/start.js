const Webpack = require('webpack');
const WebpackDevServer = require('webpack-dev-server');
const config = require('../conf/webpack/webpack.config.dev.js');

const compiler = Webpack(config);

const devServerOptions = {
  ...config.devServer,
  open: false,
};

const server = new WebpackDevServer(devServerOptions, compiler);

server.startCallback(() => {
  console.log('🚀 Dev server running at http://localhost:3000/');
});