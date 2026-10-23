import { clsx } from 'clsx'
import { twMerge } from 'tailwind-merge'

/** 合并 Tailwind 类名：后写的冲突类覆盖先写的（shadcn 同款实现） */
export function cn(...inputs) {
  return twMerge(clsx(inputs))
}
