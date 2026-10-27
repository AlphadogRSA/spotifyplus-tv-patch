import dev.aurora.tvpatch.HushPolicy;
public class HushPolicyTest {
 public static void main(String[] args) {
  assert HushPolicy.attempt(true,true,true,true,1000000,0,0,0,0);
  assert !HushPolicy.attempt(false,true,true,true,1000000,0,0,0,0);
  assert !HushPolicy.attempt(true,false,true,true,1000000,0,0,0,0);
  assert !HushPolicy.attempt(true,true,true,false,1000000,0,0,0,0);
  assert !HushPolicy.attempt(true,true,false,true,1000000,0,0,0,0);
  assert !HushPolicy.attempt(true,true,true,true,1000000,999000,0,0,0);
  assert !HushPolicy.attempt(true,true,true,true,1000000,900000,999000,0,0);
  assert !HushPolicy.attempt(true,true,true,true,1000000,0,0,999000,0);
  assert !HushPolicy.attempt(true,true,true,true,1000000,0,0,0,1000000);
  assert HushPolicy.attempt(true,true,true,true,1000000,900000,800000,0,1000001);
  System.out.println("HushPolicyTest: opt-in, helper loss, pause, cooldown, pending and sleep passed");
 }
}
