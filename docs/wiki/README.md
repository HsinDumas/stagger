# Stagger Wiki

This is the documentation hub for practical Stagger usage and configuration.

## Quick Navigation

### English

- [stagger.json Configuration Reference](./stagger-json.md)

### 中文

- [stagger.json 配置参考](./stagger-json-cn.md)

## Entry by Role

- New user: Start from the `Minimal Working Example` section in the stagger.json reference.
- Integrator: Read `Where stagger.json Is Read` and `Build Commands` first.
- Maintainer: Keep this wiki aligned with source-of-truth classes and plugin behavior.

## Related Docs

- [Migration Notes](../CODEX_MIGRATION_PLAN.md)
- [Contributing Guide](../../CONTRIBUTING.md)
- [Project README (EN)](../../README.md)
- [Project README (中文)](../../README_CN.md)

## Maintenance Rules

- Keep examples runnable and aligned with current plugin options.
- Prefer additive updates over breaking heading/name changes to reduce external link breakage.
- When changing config behavior, update both language pages in the same pull request.

## Sync to GitHub Wiki

- Source of truth is this folder: `docs/wiki`.
- GitHub Wiki `Home.md` is synced from this file.
- Automatic sync is handled by workflow: `.github/workflows/sync-wiki.yml`.
