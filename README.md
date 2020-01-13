## Long-Running HTTP Operation With Polling
  The <i>callback</i> is common pattern for handling long-running operations in self-contained software systems. One component of the system issues a request to another. Some time later, on completion of the request, the recipient <i>calls</i> the requestor <i>back</i> with the results. In the meantime the requester is free to carry on, without putting the whole system on hold while it awaits the results.
<p>
  This pattern can work well in distributed systems, too--say a microservces environment where little or no distinction is made between <i>clients</i> and <i>servers</i>. But what about the internet, where a web browser makes a request for a long-running operation to a  server on the other side of the globe? The request part is easy; the callback not so much.
<p>
  For this type of interaction we need a different approach, which I will call the <i>order-for-pickup</i> pattern (whereas the traditional callback might be referred to as <i>order-for-delivery</i>).
