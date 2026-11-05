#include <jni.h>
#include <dlfcn.h>
#include <android/log.h>
#include <string>
#include <fstream>
#include <cstdlib>

#define LOG_TAG "DriverLoader"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Turnip (and most Android Vulkan ICDs) are loaded by the Vulkan loader
// via VK_ICD_FILENAMES / VK_DRIVER_FILES pointing at a minimal manifest
// JSON. No root required: this is the same mechanism adrenotools and
// Winlator use to hot-swap the vendor Vulkan driver.

extern "C" JNIEXPORT jboolean JNICALL
Java_com_fox_emu_driver_TurnipLoader_loadDriver(
        JNIEnv *env, jobject, jstring jDriverPath, jstring jIcdJsonPath) {

    const char *driverPath = env->GetStringUTFChars(jDriverPath, nullptr);
    const char *icdJsonPath = env->GetStringUTFChars(jIcdJsonPath, nullptr);

    std::ofstream icd(icdJsonPath, std::ios::trunc);
    if (!icd.is_open()) {
        LOGE("Failed to write ICD json at %s", icdJsonPath);
        env->ReleaseStringUTFChars(jDriverPath, driverPath);
        env->ReleaseStringUTFChars(jIcdJsonPath, icdJsonPath);
        return JNI_FALSE;
    }

    icd << "{\n"
        << "  \"file_format_version\": \"1.0.0\",\n"
        << "  \"ICD\": {\n"
        << "    \"library_path\": \"" << driverPath << "\",\n"
        << "    \"api_version\": \"1.3.0\"\n"
        << "  }\n"
        << "}\n";
    icd.close();

    setenv("VK_ICD_FILENAMES", icdJsonPath, 1);
    setenv("VK_DRIVER_FILES", icdJsonPath, 1);

    void *handle = dlopen(driverPath, RTLD_NOW | RTLD_GLOBAL);

    env->ReleaseStringUTFChars(jDriverPath, driverPath);
    env->ReleaseStringUTFChars(jIcdJsonPath, icdJsonPath);

    if (!handle) {
        LOGE("dlopen failed: %s", dlerror());
        return JNI_FALSE;
    }

    LOGI("Turnip driver loaded OK");
    return JNI_TRUE;
}

extern "C" JNIEXPORT void JNICALL
Java_com_fox_emu_driver_TurnipLoader_setEnv(
        JNIEnv *env, jobject, jstring jKey, jstring jVal) {
    const char *key = env->GetStringUTFChars(jKey, nullptr);
    const char *val = env->GetStringUTFChars(jVal, nullptr);
    setenv(key, val, 1);
    env->ReleaseStringUTFChars(jKey, key);
    env->ReleaseStringUTFChars(jVal, val);
}
