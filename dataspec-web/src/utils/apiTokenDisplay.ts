interface TokenScopeLike {
  allProjects?: boolean
  projectIds?: Array<number | null | undefined>
}

interface TokenCreateFormLike extends TokenScopeLike {
  name?: string
  operatorName?: string
}

export function formatTokenProjectScope(token: TokenScopeLike): string {
  if (token.allProjects) {
    return '全部项目'
  }
  const ids = token.projectIds?.filter((id): id is number => typeof id === 'number') ?? []
  return ids.length > 0 ? ids.join(', ') : '未配置'
}

export function canSubmitApiTokenForm(form: TokenCreateFormLike): boolean {
  if (!form.name?.trim() || !form.operatorName?.trim()) {
    return false
  }
  if (form.allProjects) {
    return true
  }
  return Boolean(form.projectIds?.length)
}
