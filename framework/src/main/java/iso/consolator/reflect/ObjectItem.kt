package iso.consolator.reflect

// provides two-way access to get and set
interface ObjectItem<T> : PropertyItem, ReceiverItem<T>