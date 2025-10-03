package iso.consolator.reflect

// provides access to call with getter and setter
interface CallableObjectItem<T, out R> : CallableItem<R>, ObjectItem<T>