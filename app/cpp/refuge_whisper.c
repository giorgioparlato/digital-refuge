// The calls refuge makes into whisper.cpp: load a model, transcribe 16 kHz mono audio, let it go.
// See app.bedtime.voice.Whisper.
#include <jni.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include "whisper.h"

JNIEXPORT jlong JNICALL
Java_app_bedtime_voice_Whisper_load(JNIEnv *env, jobject thiz, jstring path) {
    const char *model = (*env)->GetStringUTFChars(env, path, NULL);
    struct whisper_context_params params = whisper_context_default_params();
    params.use_gpu = false;
    struct whisper_context *ctx = whisper_init_from_file_with_params(model, params);
    (*env)->ReleaseStringUTFChars(env, path, model);
    return (jlong) (intptr_t) ctx;
}

// The words, as UTF-8 bytes (Java's own UTF-8 is not quite UTF-8); null if it failed.
JNIEXPORT jbyteArray JNICALL
Java_app_bedtime_voice_Whisper_transcribe(JNIEnv *env, jobject thiz, jlong handle, jfloatArray audio, jint threads, jstring language) {
    struct whisper_context *ctx = (struct whisper_context *) (intptr_t) handle;
    if (ctx == NULL) return NULL;
    const char *lang = (*env)->GetStringUTFChars(env, language, NULL);
    struct whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    params.language = lang;
    params.translate = false;
    params.n_threads = threads;
    params.no_context = true;
    params.no_timestamps = true;
    params.print_special = false;
    params.print_progress = false;
    params.print_realtime = false;
    params.print_timestamps = false;
    jsize n = (*env)->GetArrayLength(env, audio);
    jfloat *samples = (*env)->GetFloatArrayElements(env, audio, NULL);
    int failed = whisper_full(ctx, params, samples, n);
    (*env)->ReleaseFloatArrayElements(env, audio, samples, JNI_ABORT);
    (*env)->ReleaseStringUTFChars(env, language, lang);
    if (failed) return NULL;

    int segments = whisper_full_n_segments(ctx);
    size_t length = 0;
    for (int i = 0; i < segments; i++) length += strlen(whisper_full_get_segment_text(ctx, i));
    jbyteArray out = (*env)->NewByteArray(env, (jsize) length);
    size_t at = 0;
    for (int i = 0; i < segments; i++) {
        const char *text = whisper_full_get_segment_text(ctx, i);
        size_t len = strlen(text);
        (*env)->SetByteArrayRegion(env, out, (jsize) at, (jsize) len, (const jbyte *) text);
        at += len;
    }
    return out;
}

JNIEXPORT void JNICALL
Java_app_bedtime_voice_Whisper_free(JNIEnv *env, jobject thiz, jlong handle) {
    struct whisper_context *ctx = (struct whisper_context *) (intptr_t) handle;
    if (ctx != NULL) whisper_free(ctx);
}
