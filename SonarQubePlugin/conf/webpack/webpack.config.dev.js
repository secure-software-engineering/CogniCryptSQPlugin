const { merge } = require('webpack-merge');
const webpack = require('webpack');
const baseConfig = require('./webpack.config'); // This is webpack.config.js
const path = require('path');
// Removed HtmlWebpackPlugin as per your update

module.exports = merge(baseConfig, {
  mode: 'development',
  devtool: 'source-map',
  output: {
    // This is the CRITICAL part.
    // This publicPath tells Webpack how to prefix URLs for assets *it* bundles.
    // It also affects how your *plugin's code* resolves dynamic imports or asset URLs.
    // It should be the full URL where your plugin's static assets are served by your dev server.
    // Given the previous example, '/static/secai/' is a common convention for SonarQube plugins.
    publicPath: 'http://localhost:3000/static/secai/', // Adjusted to include plugin-specific path
    pathinfo: true,
    filename: 'sec_ai.js', // Ensure this matches your entry point name
  },
  devServer: {
    port: 3000,
    hot: true,
    liveReload: true,
    // This tells the dev server where to serve static files from.
    // The publicPath here is the URL path from which these files will be accessible.
    static: {
      directory: path.join(__dirname, '../../target/classes/static'),
      publicPath: '/static/secai/' // This path should match a portion of your output.publicPath
    },
    proxy: {
      // Proxy API calls back to SonarQube
      '/api': 'http://localhost:9000',
      '/batch': 'http://localhost:9000',
      '/project': 'http://localhost:9000',
      '/session': 'http://localhost:9000',

      // NEW / REFINED PROXY RULES:
      // These rules are to proxy SonarQube's *core* static assets
      // (like polyfills, vendor JS, main CSS) back to the SonarQube server (9000).
      // The dev server on 3000 does NOT have these files.
      // Important: Ensure these paths do NOT conflict with your own plugin's assets
      // (e.g., if you had a file at /js/myplugin.js, it might get proxied).
      // If your plugin's assets are strictly under /static/secai/, this should be fine.

      // Proxy /js/ requests (for SonarQube's core JS files)
      '/js': {
        target: 'http://localhost:9000',
        changeOrigin: true, // Needed for virtual hosting
        secure: false, // For local http, usually false
        // rewrite: (path) => path.replace(/^\/js/, '/js'), // Often not needed if path matches
      },
      // Proxy /css/ requests (for SonarQube's core CSS files)
      '/css': {
        target: 'http://localhost:9000',
        changeOrigin: true,
        secure: false,
      },
      // Proxy /static/ requests for SonarQube's other static assets
      // Make sure this doesn't accidentally proxy your plugin's /static/secai/
      '/static': {
        target: 'http://localhost:9000',
        changeOrigin: true,
        secure: false,
        // IF YOUR PLUGIN'S ASSETS ARE UNDER /static/secai/,
        // YOU MUST EXCLUDE THEM FROM THIS PROXY.
        // Option 1: More specific proxy rules for SonarQube's static assets (if known)
        // Option 2: Use a regex to exclude your plugin's path:
        // By adding `!/static/secai` it means, proxy anything under /static unless it starts with /static/secai/
        context: ['/static', '!/static/secai'],
      },
      '/fonts': {
        target: 'http://localhost:9000',
        secure: false,
        changeOrigin: true
      },
      // If you see other root-level assets getting 404s (like /favicon.ico), add them here
      // '/favicon.ico': 'http://localhost:9000',
    },
    historyApiFallback: true, // For SPA routing
    devMiddleware: {
      writeToDisk: false, // Serve from memory
    },
    allowedHosts: 'all',
    headers: {
      'Access-Control-Allow-Origin': '*', // Essential for CORS when SonarQube loads from 3000
    },
    client: {
      overlay: true,
      progress: true,
      logging: 'info',
      reconnect: true
    },
  },
  plugins: [
    new webpack.HotModuleReplacementPlugin(),
    new webpack.ProvidePlugin({
      React: 'react',
      ReactDOM: 'react-dom',
    }),
    // Removed HtmlWebpackPlugin
  ]
});