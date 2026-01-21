const path = require('path');
const MiniCssExtractPlugin = require('mini-css-extract-plugin');

module.exports = {
  mode: 'development',
  entry: {
    sec_ai: path.resolve(__dirname, '../../src/main/js/sec_ai/index.js'),
  },
  output: {
    path: path.resolve(__dirname, '../../target/classes/static'),
    filename: '[name].js',
    publicPath: '/',
  },
  resolve: {
    modules: [path.resolve(__dirname, 'src/main/js'), 'node_modules']
  },
  module: {
    rules: [
      {
        test: /\.js$/,
        exclude: /node_modules/,
        use: 'babel-loader'
      },
      {
        test: /\.css$/,
        // This is the important change.
        // We check the mode to decide which loader to use.
        // use: [
        //   process.env.NODE_ENV === 'production' ? MiniCssExtractPlugin.loader : 'style-loader',
        //   'css-loader',
        //   'postcss-loader'
        // ]

        use: ['style-loader', 'css-loader', 'postcss-loader']
      },
      {
        test: /\.(png|jpe?g|gif|svg)$/i,
        type: 'asset/resource',
        generator: {
          filename: '[name][ext]'
        }
      }
    ]
  },
  externals: {
    react: 'React',
    'react-dom': 'ReactDOM',
    'sonar-request': 'SonarRequest',
  }
};
