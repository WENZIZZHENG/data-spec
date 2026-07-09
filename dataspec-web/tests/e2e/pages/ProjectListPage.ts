import { expect, type Page } from '@playwright/test'
import { aiActionNames, stableTestIds } from '../../../src/utils/stableTestIds'
import { controlInput } from './locators'
import type { project } from '../support/routeHarness'

type ProjectInput = Pick<typeof project, 'name' | 'description'>

/**
 * 项目列表页的浏览器级页面对象。
 *
 * 封装项目创建和当前项目选择主路径；调用方只传业务字段，不依赖弹窗文案或 Element Plus 内部 DOM。
 */
export class ProjectListPage {
  /** AI browser automation 可复用的动作名称。 */
  readonly actionNames = aiActionNames.projects

  constructor(private readonly page: Page) {}

  /** 打开项目列表并等待稳定页面根节点可见。 */
  async goto() {
    await this.page.goto('/projects')
    await expect(this.page.getByTestId(stableTestIds.projects.page)).toBeVisible()
  }

  /** 创建项目并断言该项目成为当前项目。 */
  async createAndSelectProject(input: ProjectInput) {
    await this.page.getByTestId(stableTestIds.projects.newProjectButton).click()
    await controlInput(this.page, stableTestIds.projects.projectNameInput).fill(input.name)
    await controlInput(this.page, stableTestIds.projects.projectDescriptionInput).fill(input.description)
    await this.page.getByTestId(stableTestIds.projects.saveProjectButton).click()

    const projectTable = this.page.getByTestId(stableTestIds.projects.table)
    const projectRow = projectTable.getByRole('row').filter({ hasText: input.name })
    await expect(projectRow).toContainText(input.name)
    await expect(projectRow).toContainText('当前')
  }
}
