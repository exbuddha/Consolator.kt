package iso.consolator.reflect

// provides access to call with a setter
interface CallableReceiverItem<in T, out R> : CallableItem<R>, ReceiverItem<T>