module.exports = {
  extends: ['@commitlint/config-conventional'],
  rules: {
    'type-enum': [
      2,
      'always',
      ['feat', 'fix', 'refactor', 'test', 'chore', 'docs', 'build', 'ci'],
    ],
    'scope-empty': [2, 'never'],
    'scope-enum': [
      2,
      'always',
      [
        'auth',
        'portfolio',
        'orders',
        'kafka',
        'graphql',
        'agent',
        'ui',
        'db',
        'docker',
        'tooling',
        'wallet',
      ],
    ],
    'header-max-length': [2, 'always', 72],
  },
};
