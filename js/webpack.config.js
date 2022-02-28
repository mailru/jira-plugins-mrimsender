/* eslint-disable @typescript-eslint/no-var-requires */
const path = require('path');
const WrmPlugin = require('atlassian-webresource-webpack-plugin');
const BundleAnalyzerPlugin = require('webpack-bundle-analyzer').BundleAnalyzerPlugin;
const TerserPlugin = require('terser-webpack-plugin');
const WebpackBar = require('webpackbar');
const { CleanWebpackPlugin } = require('clean-webpack-plugin');
const { DuplicatesPlugin } = require('inspectpack/plugin');
const WRM_DEPENDENCIES_CONFIG = require(`./wrm-dependencies-conf.js`);

const PLUGIN_NAME = 'myteam.bot';
const PLUGIN_KEY = 'ru.mail.jira.plugins.myteam'; // current plugin key
const MVN_OUTPUT_DIR = path.join(__dirname, '..', 'target', 'classes'); // atlassian mvn plugin classes output
const FRONTEND_SRC_DIR = path.join(__dirname, 'src'); // directory with all frontend sources
const BUNDLE_OUTPUT_DIR_NAME = 'webpack_bundles'; // directory which contains all build resources (bundles)
const FRONTEND_TARGET_DIR = path.join(MVN_OUTPUT_DIR, ...PLUGIN_KEY.split('.'), BUNDLE_OUTPUT_DIR_NAME); // jira target dir for bundle outputs

//so this is an object which module.exports should return
const config = {
  target: 'web',
  context: FRONTEND_SRC_DIR, // directory where to look for all entries
  entry: {
    'create-chat-panel': [path.join(FRONTEND_SRC_DIR, 'create-chat-panel', 'index.tsx')], // build entry point
    'chat-settings-panel': [path.join(FRONTEND_SRC_DIR, 'chat-settings-panel', 'index.tsx')], // build entry point
  },
  module: {
    rules: [
      {
        // more info about ts-loader configuration here: https://github.com/TypeStrong/ts-loader
        test: /\.(ts|tsx)$/, // compiles all TypeScript files
        use: ['babel-loader', 'ts-loader'], // TypeScript loader for webpack
        exclude: /node_modules/, // excludes node_modules directory
      },
      {
        test: /\.(png)$/i,
        use: [
          {
            loader: 'url-loader',
          },
        ],
      },
    ],
  },
  plugins: [
    // Atlassian Web-Resource Webpack Plugin
    // configuration documentation: https://bitbucket.org/atlassianlabs/atlassian-webresource-webpack-plugin
    new WrmPlugin({
      pluginKey: PLUGIN_KEY, // current plugin key
      providedDependencies: WRM_DEPENDENCIES_CONFIG, // internal jira plugins web-resource dependencies
      contextMap: {
        'chat-settings-panel': [PLUGIN_KEY + '.' + 'chat.settings.panel'], // Specify in which web-resource context to include entrypoint resources
        'create-chat-panel': ['jira.browse.project', 'jira.navigator.advanced'],
      },
      verbose: false,
      xmlDescriptors: path.resolve(MVN_OUTPUT_DIR, 'META-INF', 'plugin-descriptors', 'wr-webpack-bundles.xml'), //An absolute filepath to where the generated XML should be output to
      locationPrefix: PLUGIN_KEY.split('.').join('/') + '/' + BUNDLE_OUTPUT_DIR_NAME, // Adds given prefix value to location attribute of resource node
    }),
    new WebpackBar(), // Elegant ProgressBar and Profiler for Webpack,
  ],
  externals: {
    JIRA: 'JIRA',
    AJS: {
      var: 'AJS',
    },
    jquery: 'require("jquery")',
    'wrm/context-path': 'require("wrm/context-path")',
    'jira/api/projects': 'require("jira/api/projects")',
    'wrm/format': 'AJS.format',
  },
  resolve: {
    extensions: ['.ts', '.tsx', '.js'],
    alias: {
      // All i18n calls really invokes via @atlassian/wrm-react-i18n plugin
      // @atlassian/wrm-react-i18n configuration could be found here: https://www.npmjs.com/package/@atlassian/i18n-properties-loader
      i18n: '@atlassian/wrm-react-i18n',
    },
  },
  output: {
    // more info about webpack output config here: https://webpack.js.org/configuration/output/
    filename: '[name].js', // regular file, filename
    chunkFilename: '[name].js', // chunk filename
    sourceMapFilename: 'assets/[name].js._map', //source-map filename if has any
    path: path.resolve(FRONTEND_TARGET_DIR), // directory with all output files
    chunkLoadingGlobal: 'webpackChunk_' + PLUGIN_NAME,
  },
  optimization: {
    // default code-splitting here  via  internal webpack SplitChunksPlugin
    // more info here: https://webpack.js.org/plugins/split-chunks-plugin/#optimizationsplitchunks
    chunkIds: 'named',
    runtimeChunk: {
      name: 'manifest',
    },
    splitChunks: {
      cacheGroups: {
        vendor: {
          test: /[\\/]node_modules[\\/]/,
          name: 'vendors',
          priority: -20,
          chunks: 'all',
        },
      },
    },
  },
};

module.exports = (env, argv) => {
  const devMode = argv.mode === 'development';
  config.mode = argv.mode;

  if (argv.mode === 'development') {
    config.watch = true;
    config.watchOptions = {
      aggregateTimeout: 2000,
    };
    config.devtool = 'source-map';
  } else if (argv.mode === 'production') {
    config.devtool = false;
    if (argv.analyze) {
      // Here we go if now is: yarn analyze command running
      config.plugins.push(new DuplicatesPlugin()); // Shows package duplicates during build, if has any
      config.plugins.push(new BundleAnalyzerPlugin()); // Shows bundle sizes analysis results tree on http://127.0.0.1:8888/
    }
    config.plugins.push(new CleanWebpackPlugin());
  }

  config.optimization = {
    ...config.optimization,
    minimizer: [
      new TerserPlugin({
        terserOptions: {
          mangle: {
            // Don't mangle usage of I18n.getText() function
            reserved: ['I18n', 'getText'], // Part of @atlassian/wrm-react-i18n configuration
          },
          parallel: true, // runs all js minification tasks in parallel threads
          sourceMap: !devMode,
        },
      }),
    ],
  };

  return config;
};
