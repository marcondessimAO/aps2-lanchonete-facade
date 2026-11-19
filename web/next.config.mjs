const apiJava = (process.env.API_JAVA_URL ?? 'http://127.0.0.1:8080').replace(/\/$/, '');

export default {
  async rewrites() {
    return [{ source: '/api/:rota*', destination: `${apiJava}/api/:rota*` }];
  },
};
