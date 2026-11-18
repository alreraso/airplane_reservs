import { createContext, useContext } from 'react'

export type ShowToast = (message: string, isError?: boolean) => void

export const ToastContext = createContext<ShowToast>(() => {})

export const useToast = (): ShowToast => useContext(ToastContext)
