package dev.stow.client.ui;

/** Time-based cubic easing and high-resolution wheel accumulation; no delayed clicks or queued pages. */
public final class ListMotion {
    private double wheel;
    private float from;
    private long started;
    private int index;
    public int wheelSteps(double amount){
        if(Math.signum(wheel)!=Math.signum(amount))wheel=0;
        wheel+=amount;int steps=(int)wheel;wheel-=steps;
        return Math.clamp(steps,-4,4);
    }
    public void moveTo(int next,int rowHeight,int columns){
        if(next==index)return;
        float shift=offset()+(next-index)/(float)Math.max(1,columns)*rowHeight;
        from=Math.clamp(shift,-rowHeight,rowHeight);started=System.nanoTime();index=next;
    }
    public float offset(){
        float t=Math.clamp((System.nanoTime()-started)/160_000_000f,0,1);
        return from*(1-t)*(1-t)*(1-t);
    }
    public void reset(int next){index=next;from=0;wheel=0;started=0;}
}
