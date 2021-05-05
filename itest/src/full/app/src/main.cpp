#include "main$package$.h"
#include "lib1.h"

JNIEXPORT jint JNICALL Java_main_00024package_00024_answer
  (JNIEnv *env, jobject instance) {

  return answer_to_everything();

}
