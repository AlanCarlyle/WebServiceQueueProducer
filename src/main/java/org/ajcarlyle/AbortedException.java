package org.ajcarlyle;


public class AbortedException extends RuntimeException {

  private static final long serialVersionUID = -2391068728995041250L;

  public AbortedException(String message) {
    super(message);
  }
  public AbortedException(Throwable e){
    super(e);
  }
  public AbortedException(String message,Throwable e){
    super(message, e);
  }
  
}