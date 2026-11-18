// Anonymous identity of this browser TAB. sessionStorage is per tab, so two tabs behave
// like two different customers, which is what we want when demoing real time.
const KEY = 'clientId'

export function getClientId(): string {
  let id = sessionStorage.getItem(KEY)
  if (!id) {
    id = crypto.randomUUID()
    sessionStorage.setItem(KEY, id)
  }
  return id
}
