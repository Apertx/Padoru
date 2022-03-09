package apertx.padoru;
import android.app.*;
import android.media.*;
import android.opengl.*;
import android.os.*;
import android.view.*;
import android.view.View.*;
import java.io.*;
import java.nio.*;
import java.util.*;
import javax.microedition.khronos.opengles.*;

import static android.opengl.GLES20.*;

public class PadoruActivity extends Activity implements GLSurfaceView.Renderer,OnTouchListener{
 protected void onCreate(Bundle b){
  super.onCreate(b);
  r=new Random();
  glsv=new GLSurfaceView(this);
  glsv.setEGLContextClientVersion(2);
  glsv.setEGLConfigChooser(false);
  glsv.setRenderer(this);
  glsv.setOnTouchListener(this);
  glsv.setRenderMode(glsv.RENDERMODE_WHEN_DIRTY);
  setContentView(glsv);
  bb=ByteBuffer.allocateDirect(96);
  bb.order(ByteOrder.nativeOrder());
  fb=bb.asFloatBuffer();
  fb.put(
   new float[]{
    -1.0f,+1.0f,0.0f,0.0f,
    +1.0f,-1.0f,1.0f,1.0f,
    +1.0f,+1.0f,1.0f,0.0f,
    -1.0f,+1.0f,0.0f,0.0f,
    -1.0f,-1.0f,0.0f,1.0f,
    +1.0f,-1.0f,1.0f,1.0f
   });
  bb=ByteBuffer.allocateDirect(PARTS*24);
  bb.order(ByteOrder.nativeOrder());
  pb=bb.asFloatBuffer();
  for(int i=0;i<PARTS;i++)pb.put(
    new float[]{
     r.nextFloat()*2-1,r.nextFloat()*2-1,
     r.nextFloat()*4,r.nextFloat()/4+0.25f,
     SystemClock.uptimeMillis()/1000.0f,
     SystemClock.uptimeMillis()/1000.0f+15.0f
    });
  padoru=new ArrayList<Padoru>();
  han=new Handler(){
   public void handleMessage(Message m){
    if(m.what==1){
     han.removeMessages(1);
     han.sendEmptyMessageDelayed(1,40);
     glsv.requestRender();
    }
   }
  };
  mp=MediaPlayer.create(this,R.raw.music);
  mp.setLooping(true);
 }

 public void onSurfaceCreated(GL10 gl,javax.microedition.khronos.egl.EGLConfig conf){
  prog=new int[2];
  tex=new int[2];
  glb=new int[1];
  glGenTextures(2,tex,0);
  glActiveTexture(GL_TEXTURE0);

  prog[0]=createProgram(R.raw.padoruv,R.raw.padoruf);
  glUseProgram(prog[0]);
  aPos=glGetAttribLocation(prog[0],"aPos");
  aTex=glGetAttribLocation(prog[0],"aTex");
  uMat=glGetUniformLocation(prog[0],"uMat");
  uTex=glGetUniformLocation(prog[0],"uTex");
  glEnableVertexAttribArray(aPos);
  glEnableVertexAttribArray(aTex);

  prog[1]=createProgram(R.raw.snowv,R.raw.snowf);
  glUseProgram(prog[1]);
  aSPos=glGetAttribLocation(prog[1],"aSPos");
  aSVec=glGetAttribLocation(prog[1],"aSVec");
  aSLifeT=glGetAttribLocation(prog[1],"aSLifeT");
  uSTime=glGetUniformLocation(prog[1],"uSTime");
  uSTex=glGetUniformLocation(prog[1],"uSTex");
  glGenBuffers(1,glb,0);
  glEnableVertexAttribArray(aSPos);
  glEnableVertexAttribArray(aSVec);
  glEnableVertexAttribArray(aSLifeT);

  glClearColor(0.125f,0.5f,0.25f,1.0f);
  glBlendFunc(GL_SRC_ALPHA,GL_ONE_MINUS_SRC_ALPHA);
  glEnable(GL_BLEND);
  glEnable(GL_CULL_FACE);
  glDisable(GL_DITHER);

  glBindTexture(GL_TEXTURE_2D,tex[0]);
  glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_NEAREST_MIPMAP_NEAREST);
  glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_LINEAR);
  glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_S,GL_CLAMP_TO_EDGE);
  glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_T,GL_CLAMP_TO_EDGE);
  InputStream is=getResources().openRawResource(R.raw.padoru);
  byte[]buf=new byte[0];
  try{
   buf=new byte[is.available()];
   is.read(buf);
   is.close();
  }catch(Exception e){}
  bb=ByteBuffer.allocateDirect(buf.length);
  bb.put(buf);
  bb.position(0);
  glTexImage2D(GL_TEXTURE_2D,0,GL_RGBA,512,512,0,GL_RGBA,GL_UNSIGNED_SHORT_5_5_5_1,bb);
  glGenerateMipmap(GL_TEXTURE_2D);

  glBindTexture(GL_TEXTURE_2D,tex[1]);
  glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_NEAREST_MIPMAP_NEAREST);
  glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_LINEAR);
  glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_S,GL_CLAMP_TO_EDGE);
  glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_WRAP_T,GL_CLAMP_TO_EDGE);
  is=getResources().openRawResource(R.raw.snow);
  buf=new byte[0];
  try{
   buf=new byte[is.available()];
   is.read(buf);
   is.close();
  }catch(Exception e){}
  bb=ByteBuffer.allocateDirect(buf.length);
  bb.put(buf);
  bb.position(0);
  glTexImage2D(GL_TEXTURE_2D,0,GL_RGBA,64,64,0,GL_RGBA,GL_UNSIGNED_SHORT_5_5_5_1,bb);
  glGenerateMipmap(GL_TEXTURE_2D);

  glEnable(GL_TEXTURE_2D);
  bb.clear();

  han.sendEmptyMessage(1);
 }

 public void onSurfaceChanged(GL10 gl,int w,int h){
  width=w;
  height=h;
  glViewport(0,0,w,h);
  if(w>h){
   vw=(float)h/w;
   vh=1.0f;
  }else{
   vw=1.0f;
   vh=(float)w/h;
  }
  pw=(w>>>1)*vw;
  ph=(h>>>1)*vh;
 }

 public void onDrawFrame(GL10 gl){
  pb.position(bos=(bos+6)%(PARTS*6));
  pb.put(
   new float[]{
    r.nextFloat()*2-1,1.0f,
    r.nextFloat()*4,r.nextFloat()/4+0.25f,
    SystemClock.uptimeMillis()/1000.0f,
    SystemClock.uptimeMillis()/1000.0f+15.0f
   });

  glClear(GL_COLOR_BUFFER_BIT);
  glUseProgram(prog[1]);
  glBindBuffer(GL_ARRAY_BUFFER,glb[0]);
  pb.position(0);
  glBufferData(GL_ARRAY_BUFFER,PARTS*24,pb,GL_DYNAMIC_DRAW);
  glVertexAttribPointer(aSPos,2,GL_FLOAT,false,24,0);
  glVertexAttribPointer(aSVec,2,GL_FLOAT,false,24,8);
  glVertexAttribPointer(aSLifeT,2,GL_FLOAT,false,24,16);
  glBindBuffer(GL_ARRAY_BUFFER,0);
  glUniform1i(uSTex,0);
  float nt=SystemClock.uptimeMillis()/1000.0f;
  glUniform1f(uSTime,nt);
  glBindTexture(GL_TEXTURE_2D,tex[1]);
  glDrawArrays(GL_POINTS,0,PARTS);

  glUseProgram(prog[0]);
  fb.position(0);
  glVertexAttribPointer(aPos,2,GL_FLOAT,false,16,fb);
  fb.position(2);
  glVertexAttribPointer(aTex,2,GL_FLOAT,false,16,fb);
  glUniform1i(uTex,0);
  int pl=padoru.size();
  for(int i=0;i<pl;i++)padoru.get(i).draw(gl,uMat,vw,vh);
 }

 public boolean onTouch(View v,MotionEvent e){
  switch(e.getAction()){
   case e.ACTION_MOVE:
    break;
   case e.ACTION_DOWN:
    float px=e.getX()/pw-1/vw;
    float py=1/vh-e.getY()/ph;
    padoru.add(new Padoru(tex[0],px,py,r.nextFloat()/4.0f+0.125f,r.nextInt(16)+4));
    break;
   case e.ACTION_UP:
    break;
  }
  return true;
 }

 protected void onResume(){
  super.onResume();
  glsv.onResume();
  mp.start();
 }
 protected void onPause(){
  super.onPause();
  glsv.onPause();
  mp.pause();
  han.removeMessages(1);
 }

 private int createProgram(int vsId,int fsId){
  int id=glCreateProgram();
  InputStream is=getResources().openRawResource(vsId);
  byte[]buf=new byte[0];
  try{
   buf=new byte[is.available()];
   is.read(buf);
   is.close();
  }catch(Exception e){}
  String str=new String(buf);
  int sid=glCreateShader(GL_VERTEX_SHADER);
  glShaderSource(sid,str);
  glCompileShader(sid);
  glAttachShader(id,sid);
  is=getResources().openRawResource(fsId);
  try{
   buf=new byte[is.available()];
   is.read(buf);
   is.close();
  }catch(Exception e){}
  str=new String(buf);
  sid=glCreateShader(GL_FRAGMENT_SHADER);
  glShaderSource(sid,str);
  glCompileShader(sid);
  glAttachShader(id,sid);
  glLinkProgram(id);
  glReleaseShaderCompiler();
  return id;
 }
 private GLSurfaceView glsv;
 private Handler han;
 private int aPos;
 private int aTex;
 private int uMat;
 private int uTex;
 private int aSPos;
 private int aSVec;
 private int aSLifeT;
 private int uSTime;
 private int uSTex;
 private List<Padoru>padoru;
 private float vw;
 private float vh;
 private float pw;
 private float ph;
 private int width;
 private int height;
 private FloatBuffer fb;
 private ByteBuffer bb;
 private FloatBuffer pb;
 private int[]tex;
 private int[]prog;
 private int[]glb;
 private int bos;
 private Random r;
 private MediaPlayer mp;

 private final static int PARTS=256;
}
