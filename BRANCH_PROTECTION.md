# 分支保护规则配置

本项目建议配置以下分支保护规则以确保代码质量和稳定性。

## 自动配置（推荐）

使用 GitHub CLI：

```bash
gh repo rule create --branch main \
  --require-status-checks \
  --require-code-reviews \
  --dismiss-stale-reviews \
  --require-linear-history
```

## 手动配置步骤

1. 访问 Repository Settings → Branches
2. 点击 "Add rule" 或选择 main 分支进行配置

### 建议的保护规则设置

#### Require status checks to pass before merging
- ✅ **Require branches to be up to date before merging**
- ✅ **Require status checks to pass before merging**
  - Select: `build` (Java 8, 11, 17)
  - Select: `code-quality`

#### Require a pull request before merging
- ✅ **Require pull request reviews before merging**
  - Require approvals: `1`
  - ✅ Dismiss stale pull request approvals when new commits are pushed
  - ✅ Require approval of the most recent reviewable push

#### Restrict who can push to matching branches
- ✅ **Restrict who can push to matching branches**
  - Only allow: `HsinDumas` (yourself)

#### Rules to enforce
- ✅ **Require linear history** - 防止 merge commits，要求 rebase
- ✅ **Require up-to-date branches before merging**
- ✅ **Include administrators** - 保护规则对所有人（包括管理员）生效

## CI/CD Workflow

项目配置了自动 CI 流程：

- **触发时机**: Push 到 main 或创建 Pull Request
- **测试环境**: Java 8, 11, 17
- **检查项**:
  - ✅ Maven 构建（stagger-core, stagger-maven-plugin）
  - ✅ 单元测试
  - ✅ Gradle 构建（stagger-gradle-plugin）
  - ✅ 代码风格检查（可选 SonarCloud）

## GitHub Secrets 配置（可选）

如果使用 SonarCloud 代码质量分析，需要配置：

1. 访问 Repository Settings → Secrets and variables → Actions
2. 添加 `SONAR_TOKEN` - 从 [SonarCloud](https://sonarcloud.io) 获取

## 常见场景

### 直接 push 到 main
❌ 不允许 - 必须通过 Pull Request

### 合并 Pull Request
- 需要通过所有 CI 检查
- 需要至少 1 个 Approval
- 可选：需要 CODEOWNERS 审查

### 强制 push
❌ 不允许

### 删除分支
✅ 允许（只影响非保护分支）
