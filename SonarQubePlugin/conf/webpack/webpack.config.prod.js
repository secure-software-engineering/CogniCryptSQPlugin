const webpack = require('webpack');
const { merge } = require('webpack-merge');
const baseConfig = require('./webpack.config');
// ADD this line at the top
// const MiniCssExtractPlugin = require('mini-css-extract-plugin')

module.exports = merge(baseConfig, {
  mode: 'production',
  devtool: 'source-map',
  // START: ADD THIS SECTION
  output: {
    // Set the public path to be relative. 
    // This makes the asset links (e.g., in <script> tags) relative to the HTML file.
    publicPath: './' 
  },
  plugins: [
    // This extracts CSS into separate files
    // new MiniCssExtractPlugin({
    //   filename: '[name].css', // Creates a css file named after your entry point, e.g., sec_ai.css
    //   chunkFilename: '[id].css'
    // }),
    new webpack.DefinePlugin({
      'process.env.NODE_ENV': JSON.stringify('production')
    })
  ] 

});
