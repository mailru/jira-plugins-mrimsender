module.exports = ({ env }) => {
  const isDev = env === 'development';
  const isProd = !isDev;

  const plugins = {
    'postcss-import': {},
    'postcss-mixins': {},
    'postcss-for': {},
    'postcss-nested': {},
    'postcss-custom-media': {},
  };

  if (isProd) {
    plugins['postcss-preset-env'] = {};
  }

  return {
    plugins,
    sourceMap: isDev,
  };
};
