export interface StandardSnapshotLike {
  specVersion?: string
  specHash?: string | null
  versioned?: boolean
}

export interface StandardSnapshotFormLike {
  version?: string
}

export function formatSnapshotLabel(snapshot: StandardSnapshotLike | null | undefined): string {
  if (!snapshot || !snapshot.versioned) {
    return '未创建快照'
  }
  const hash = snapshot.specHash ? ` (${snapshot.specHash.slice(0, 8)})` : ''
  return `${snapshot.specVersion || '未命名版本'}${hash}`
}

export function canSubmitSnapshotForm(form: StandardSnapshotFormLike): boolean {
  return Boolean(form.version?.trim())
}
