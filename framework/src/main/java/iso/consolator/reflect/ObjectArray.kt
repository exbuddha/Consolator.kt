package iso.consolator.reflect

// provides array access to get and set
interface ObjectArray<I : Number> : PropertyArray<I>, ReceiverArray<I>