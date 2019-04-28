package org.ajcarlyle.client;

import java.io.Serializable;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedLongSynchronizer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.AbstractQueuedLongSynchronizer.ConditionObject;


public class FunkyCondition<T> implements Condition, Serializable {

  private static final long serialVersionUID = -8707431819475164718L;

  private Condition condition;
  private T object;
  public FunkyCondition(Condition condition) {
    this.condition = condition;

    this.object = null;
  }

 

  /**
   * @return the message
   */
  public T getMessage() {
    return object;
  }

  /**
   * @param message the message to set
   */
  public void setMessage(T object) {
    this.object = object;
  }



public void signal(T object) {
    this.object  = object;
    condition.signal();
  }
  public void signalAll(T object) {
    this.object  = object;
    condition.signalAll();
  }




@Override
public void await() throws InterruptedException {
	await();
}
@Override
public void awaitUninterruptibly() {
	awaitUninterruptibly();
}
@Override
public long awaitNanos(long nanosTimeout) throws InterruptedException {
	return awaitNanos(nanosTimeout);
}
@Override
public boolean await(long time, TimeUnit unit) throws InterruptedException {
	return await(time, unit);
}
@Override
public boolean awaitUntil(Date deadline) throws InterruptedException {
 return condition.awaitUntil(deadline);
}
@Override
public void signal() {
	condition.signal();
}
@Override
public void signalAll() {
	condition.signalAll();
}

}

  
