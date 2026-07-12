import { nextTick, shallowRef, watch, type Ref } from 'vue'

/**
 * 记录弹窗打开前的焦点元素，并在弹窗关闭后恢复焦点。
 *
 * 约束：只恢复仍在当前 document 中的 HTMLElement，避免弹窗关闭后键盘用户丢失原操作位置。
 */
export function useDialogFocusReturn(visible: Ref<boolean>) {
  const triggerElement = shallowRef<HTMLElement | null>(null)

  watch(visible, async (isVisible) => {
    if (isVisible) {
      triggerElement.value ??= currentFocusedElement()
    }
  }, { flush: 'post' })

  return {
    rememberFocus: () => {
      triggerElement.value = currentFocusedElement()
    },
    restoreFocus: async () => {
      const target = triggerElement.value
      triggerElement.value = null
      await nextTick()
      if (target && document.contains(target)) {
        target.focus({ preventScroll: true })
      }
    }
  }
}

function currentFocusedElement() {
  if (typeof document === 'undefined') {
    return null
  }
  const activeElement = document.activeElement
  return activeElement instanceof HTMLElement ? activeElement : null
}
