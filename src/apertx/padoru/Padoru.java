package apertx.padoru;
import android.opengl.*;
import javax.microedition.khronos.opengles.*;

public class Padoru{
 public Padoru(int tex,float x,float y,float s,int r){
  this.tex=tex;
  this.x=x;
  this.y=y;
  this.s=s;
  this.r=r;
 }
 public void draw(GL10 gl,int uMat,float vw,float vh){
  Matrix.setIdentityM(mtx,0);
  Matrix.scaleM(mtx,0,vw,vh,0.0f);
  Matrix.translateM(mtx,0,x,y,0.0f);
  Matrix.rotateM(mtx,0,(System.currentTimeMillis()/r)%360,0,0,1);
  Matrix.scaleM(mtx,0,s,s,0.0f);
  GLES20.glUniformMatrix4fv(uMat,1,false,mtx,0);
  gl.glBindTexture(gl.GL_TEXTURE_2D,tex);
  gl.glDrawArrays(gl.GL_TRIANGLES,0,6);
 }

 private final float[]mtx=new float[16];
 private int tex;
 private float s;
 private float x;
 private float y;
 private int r;
}
