#include "org_telegram_messenger_voip_Instance.h"

#include <jni.h>
#include <sdk/android/native_api/video/wrapper.h>
#include <VideoCapturerInterface.h>
#include <platform/android/AndroidInterface.h>
#include <platform/android/AndroidContext.h>
#include <rtc_base/ssl_adapter.h>
#include <modules/utility/include/jvm_android.h>
#include <sdk/android/native_api/base/init.h>
#include <voip/webrtc/media/base/media_constants.h>
#include <tgnet/FileLog.h>
#include <voip/tgcalls/group/GroupInstanceCustomImpl.h>

#include <memory>
#include <utility>
#include <map>

#include "pc/video_track.h"
#include "legacy/InstanceImplLegacy.h"
#include "InstanceImpl.h"
#include "reference/InstanceImplReference.h"
#include "libtgvoip/os/android/AudioOutputOpenSLES.h"
#include "libtgvoip/os/android/AudioInputOpenSLES.h"
#include "libtgvoip/os/android/JNIUtilities.h"
#include "tgcalls/VideoCaptureInterface.h"

using namespace tgcalls;

const auto RegisterTag = Register<InstanceImpl>();
const auto RegisterTagLegacy = Register<InstanceImplLegacy>();
const auto RegisterTagReference = tgcalls::Register<InstanceImplReference>();

jclass TrafficStatsClass;
jclass FingerprintClass;
jclass FinalStateClass;
jclass NativeInstanceClass;
jmethodID FinalStateInitMethod;

class RequestMediaChannelDescriptionTaskJava : public RequestMediaChannelDescriptionTask {
public:
    RequestMediaChannelDescriptionTaskJava(std::shared_ptr<PlatformContext> platformContext,
                                           std::function<void(std::vector<MediaChannelDescription> &&)> callback) :
        _platformContext(std::move(platformContext)),
        _callback(std::move(callback)) {
    }

    void call(JNIEnv *env, jintArray audioSsrcs) {
        std::vector<MediaChannelDescription> descriptions;

        jint *ssrcsArr = env->GetIntArrayElements(audioSsrcs, nullptr);
        jsize size = env->GetArrayLength(audioSsrcs);
        for (int i = 0; i < size; i++) {
            MediaChannelDescription description;
            description.type = MediaChannelDescription::Type::Audio;
            description.audioSsrc = ssrcsArr[i];
            descriptions.push_back(description);
        }
        env->ReleaseIntArrayElements(audioSsrcs, ssrcsArr, JNI_ABORT);
        _callback(std::move<>(descriptions));
    }

private:
    void cancel() override {
        /*tgvoip::jni::DoWithJNI([&](JNIEnv *env) {
            jobject globalRef = ((AndroidContext *) _platformContext.get())->getJavaInstance();
            env->CallVoidMethod(globalRef, env->GetMethodID(NativeInstanceClass, "onCancelRequestMediaChannelDescription", "(J)V"), _timestamp);
        });*/
    }

    std::shared_ptr<PlatformContext> _platformContext;
    std::function<void(std::vector<MediaChannelDescription> &&)> _callback;
};

class BroadcastPartTaskJava : public BroadcastPartTask {
public:
    BroadcastPartTaskJava(std::shared_ptr<PlatformContext> platformContext,
            std::function<void(BroadcastPart &&)> callback,
            int64_t timestamp) :
            _platformContext(std::move(platformContext)),
            _callback(std::move(callback)),
            _timestamp(timestamp) {
    }

    void call(int64_t ts, int64_t responseTs, BroadcastPart::Status status, uint8_t *data, int32_t len) {
        if (_timestamp != ts) {
            return;
        }
        BroadcastPart part;
        part.timestampMilliseconds = _timestamp;
        part.responseTimestamp = responseTs / 1000.0;
        part.status = status;
        if (data != nullptr) {
            part.oggData = std::vector<uint8_t>(data, data + len);
        }
        _callback(std::move<>(part));
    }

private:
    void cancel() override {
        tgvoip::jni::DoWithJNI([&](JNIEnv *env) {
            jobject globalRef = ((AndroidContext *) _platformContext.get())->getJavaInstance();
            env->CallVoidMethod(globalRef, env->GetMethodID(NativeInstanceClass, "onCancelRequestBroadcastPart", "(J)V"), _timestamp);
        });
    }

    std::shared_ptr<PlatformContext> _platformContext;
    std::function<void(BroadcastPart &&)> _callback;
    int64_t _timestamp;
};

class JavaObject {
private:
    JNIEnv *env;
    jobject obj;
    jclass clazz;

public:
    JavaObject(JNIEnv *env, jobject obj) : JavaObject(env, obj, env->GetObjectClass(obj)) {
    }

    JavaObject(JNIEnv *env, jobject obj, jclass clazz) {
        this->env = env;
        this->obj = obj;
        this->clazz = clazz;
    }

    jint getIntField(const char *name) {
        return env->GetIntField(obj, env->GetFieldID(clazz, name, "I"));
    }

    jlong getLongField(const char *name) {
        return env->GetLongField(obj, env->GetFieldID(clazz, name, "J"));
    }

    jboolean getBooleanField(const char *name) {
        return env->GetBooleanField(obj, env->GetFieldID(clazz, name, "Z"));
    }

    jdouble getDoubleField(const char *name) {
        return env->GetDoubleField(obj, env->GetFieldID(clazz, name, "D"));
    }

    jbyteArray getByteArrayField(const char *name) {
        return (jbyteArray) env->GetObjectField(obj, env->GetFieldID(clazz, name, "[B"));
    }

    jintArray getIntArrayField(const char *name) {
        return (jintArray) env->GetObjectField(obj, env->GetFieldID(clazz, name, "[I"));
    }

    jstring getStringField(const char *name) {
        return (jstring) env->GetObjectField(obj, env->GetFieldID(clazz, name, "Ljava/lang/String;"));
    }

    jobjectArray getObjectArrayField(const char *name) {
        return (jobjectArray) env->GetObjectField(obj, env->GetFieldID(clazz, name, "[Ljava/lang/Object;"));
    }
};

struct SetVideoSink {
    std::shared_ptr<rtc::VideoSinkInterface<webrtc::VideoFrame>> sink;
    VideoChannelDescription::Quality quality;
    std::string endpointId;
    std::vector<MediaSsrcGroup> ssrcGroups;
};

struct InstanceHolder {
    std::unique_ptr<Instance> nativeInstance;
    std::unique_ptr<GroupInstanceCustomImpl> groupNativeInstance;
    std::shared_ptr<tgcalls::VideoCaptureInterface> _videoCapture;
    std::shared_ptr<tgcalls::VideoCaptureInterface> _screenVideoCapture;
    std::shared_ptr<PlatformContext> _platformContext;
    std::map<std::string, SetVideoSink> remoteGroupSinks;
    bool useScreencast = false;
};

jlong getInstanceHolderId(JNIEnv *env, jobject obj) {
    return env->GetLongField(obj, env->GetFieldID(NativeInstanceClass, "nativePtr", "J"));
}

InstanceHolder *getInstanceHolder(JNIEnv *env, jobject obj) {
    return reinterpret_cast<InstanceHolder *>(getInstanceHolderId(env, obj));
}

jint throwNewJavaException(JNIEnv *env, const char *className, const char *message) {
    return env->ThrowNew(env->FindClass(className), message);
}

jint throwNewJavaIllegalArgumentException(JNIEnv *env, const char *message) {
    return throwNewJavaException(env, "java/lang/IllegalStateException", message);
}

jbyteArray copyVectorToJavaByteArray(JNIEnv *env, const std::vector<uint8_t> &bytes) {
    unsigned int size = bytes.size();
    jbyteArray bytesArray = env->NewByteArray(size);
    env->SetByteArrayRegion(bytesArray, 0, size, (jbyte *) bytes.data());
    return bytesArray;
}

void readPersistentState(const char *filePath, PersistentState &persistentState) {
    FILE *persistentStateFile = fopen(filePath, "r");
    if (persistentStateFile) {
        fseek(persistentStateFile, 0, SEEK_END);
        auto len = static_cast<size_t>(ftell(persistentStateFile));
        fseek(persistentStateFile, 0, SEEK_SET);
        if (len < 1024 * 512 && len > 0) {
            auto *buffer = static_cast<uint8_t *>(malloc(len));
            fread(buffer, 1, len, persistentStateFile);
            persistentState.value = std::vector<uint8_t>(buffer, buffer + len);
            free(buffer);
        }
        fclose(persistentStateFile);
    }
}

void savePersistentState(const char *filePath, const PersistentState &persistentState) {
    FILE *persistentStateFile = fopen(filePath, "w");
    if (persistentStateFile) {
        fwrite(persistentState.value.data(), 1, persistentState.value.size(), persistentStateFile);
        fclose(persistentStateFile);
    }
}

NetworkType parseNetworkType(jint networkType) {
    switch (networkType) {
        case org_telegram_messenger_voip_Instance_NET_TYPE_GPRS:
            return NetworkType::Gprs;
        case org_telegram_messenger_voip_Instance_NET_TYPE_EDGE:
            return NetworkType::Edge;
        case org_telegram_messenger_voip_Instance_NET_TYPE_3G:
            return NetworkType::ThirdGeneration;
        case org_telegram_messenger_voip_Instance_NET_TYPE_HSPA:
            return NetworkType::Hspa;
        case org_telegram_messenger_voip_Instance_NET_TYPE_LTE:
            return NetworkType::Lte;
        case org_telegram_messenger_voip_Instance_NET_TYPE_WIFI:
            return NetworkType::WiFi;
        case org_telegram_messenger_voip_Instance_NET_TYPE_ETHERNET:
            return NetworkType::Ethernet;
        case org_telegram_messenger_voip_Instance_NET_TYPE_OTHER_HIGH_SPEED:
            return NetworkType::OtherHighSpeed;
        case org_telegram_messenger_voip_Instance_NET_TYPE_OTHER_LOW_SPEED:
            return NetworkType::OtherLowSpeed;
        case org_telegram_messenger_voip_Instance_NET_TYPE_DIALUP:
            return NetworkType::Dialup;
        case org_telegram_messenger_voip_Instance_NET_TYPE_OTHER_MOBILE:
            return NetworkType::OtherMobile;
        default:
            return NetworkType::Unknown;
    }
}

DataSaving parseDataSaving(JNIEnv *env, jint dataSaving) {
    switch (dataSaving) {
        case org_telegram_messenger_voip_Instance_DATA_SAVING_NEVER:
            return DataSaving::Never;
        case org_telegram_messenger_voip_Instance_DATA_SAVING_MOBILE:
            return DataSaving::Mobile;
        case org_telegram_messenger_voip_Instance_DATA_SAVING_ALWAYS:
            return DataSaving::Always;
        case org_telegram_messenger_voip_Instance_DATA_SAVING_ROAMING:
            throwNewJavaIllegalArgumentException(env, "DATA_SAVING_ROAMING is not supported");
            return DataSaving::Never;
        default:
            throwNewJavaIllegalArgumentException(env, "Unknown data saving constant: " + dataSaving);
            return DataSaving::Never;
    }
}

EndpointType parseEndpointType(JNIEnv *env, jint endpointType) {
    switch (endpointType) {
        case org_telegram_messenger_voip_Instance_ENDPOINT_TYPE_INET:
            return EndpointType::Inet;
        case org_telegram_messenger_voip_Instance_ENDPOINT_TYPE_LAN:
            return EndpointType::Lan;
        case org_telegram_messenger_voip_Instance_ENDPOINT_TYPE_TCP_RELAY:
            return EndpointType::TcpRelay;
        case org_telegram_messenger_voip_Instance_ENDPOINT_TYPE_UDP_RELAY:
            return EndpointType::UdpRelay;
        default:
            throwNewJavaIllegalArgumentException(env, std::string("Unknown endpoint type: ").append(std::to_string(endpointType)).c_str());
            return EndpointType::UdpRelay;
    }
}

jint asJavaState(const State &state) {
    switch (state) {
        case State::WaitInit:
            return org_telegram_messenger_voip_Instance_STATE_WAIT_INIT;
        case State::WaitInitAck:
            return org_telegram_messenger_voip_Instance_STATE_WAIT_INIT_ACK;
        case State::Established:
            return org_telegram_messenger_voip_Instance_STATE_ESTABLISHED;
        case State::Failed:
            return org_telegram_messenger_voip_Instance_STATE_FAILED;
        case State::Reconnecting:
            return org_telegram_messenger_voip_Instance_STATE_RECONNECTING;
    }
}

jobject asJavaTrafficStats(JNIEnv *env, const TrafficStats &trafficStats) {
    jmethodID initMethodId = env->GetMethodID(TrafficStatsClass, "<init>", "(JJJJ)V");
    return env->NewObject(TrafficStatsClass, initMethodId, (jlong) trafficStats.bytesSentWifi, (jlong) trafficStats.bytesReceivedWifi, (jlong) trafficStats.bytesSentMobile, (jlong) trafficStats.bytesReceivedMobile);
}

jobject asJavaFinalState(JNIEnv *env, const FinalState &finalState) {
    jbyteArray persistentState = copyVectorToJavaByteArray(env, finalState.persistentState.value);
    jstring debugLog = env->NewStringUTF(finalState.debugLog.c_str());
    jobject trafficStats = asJavaTrafficStats(env, finalState.trafficStats);
    auto isRatingSuggested = static_cast<jboolean>(finalState.isRatingSuggested);
    return env->NewObject(FinalStateClass, FinalStateInitMethod, persistentState, debugLog, trafficStats, isRatingSuggested);
}

jobject asJavaFingerprint(JNIEnv *env, const std::string& hash, const std::string& setup, const std::string& fingerprint) {
    jstring hashStr = env->NewStringUTF(hash.c_str());
    jstring setupStr = env->NewStringUTF(setup.c_str());
    jstring fingerprintStr = env->NewStringUTF(fingerprint.c_str());
    jmethodID initMethodId = env->GetMethodID(FingerprintClass, "<init>", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");
    return env->NewObject(FingerprintClass, initMethodId, hashStr, setupStr, fingerprintStr);
}

extern "C" {

bool webrtcLoaded = false;

void initWebRTC(JNIEnv *env) {
    if (webrtcLoaded) {
        return;
    }
    JavaVM* vm;
    env->GetJavaVM(&vm);
    webrtc::InitAndroid(vm);
    webrtc::JVM::Initialize(vm);
    rtc::InitializeSSL();
    webrtcLoaded = true;

    NativeInstanceClass = static_cast<jclass>(env->NewGlobalRef(env->FindClass("org/telegram/messenger/voip/NativeInstance")));
    TrafficStatsClass = static_cast<jclass>(env->NewGlobalRef(env->FindClass("org/telegram/messenger/voip/Instance$TrafficStats")));
    FingerprintClass = static_cast<jclass>(env->NewGlobalRef(env->FindClass("org/telegram/messenger/voip/Instance$Fingerprint")));
    FinalStateClass = static_cast<jclass>(env->NewGlobalRef(env->FindClass("org/telegram/messenger/voip/Instance$FinalState")));
    FinalStateInitMethod = env->GetMethodID(FinalStateClass, "<init>", "([BLjava/lang/String;Lorg/telegram/messenger/voip/Instance$TrafficStats;Z)V");
}

JNIEXPORT jlong JNICALL Java_org_telegram_messenger_voip_NativeInstance_makeGroupNativeInstance(JNIEnv *env, jclass clazz, jobject instanceObj, jstring logFilePath, jboolean highQuality, jlong videoCapturer, jboolean screencast, jboolean noiseSupression) {
    initWebRTC(env);

    std::shared_ptr<VideoCaptureInterface> videoCapture = videoCapturer ? std::shared_ptr<VideoCaptureInterface>(reinterpret_cast<VideoCaptureInterface *>(videoCapturer)) : nullptr;

    std::shared_ptr<PlatformContext> platformContext;
    if (videoCapture) {
        platformContext = videoCapture->getPlatformContext();
        ((AndroidContext *) platformContext.get())->setJavaInstance(env, instanceObj);
    } else {
        platformContext = std::make_shared<AndroidContext>(env, instanceObj, screencast);
    }

    GroupInstanceDescriptor descriptor = {
            .threads = StaticThreads::getThreads(),
            .config = {
                    .need_log = true,
                    .logPath = {tgvoip::jni::JavaStringToStdString(env, logFilePath)},
            },
            .networkStateUpdated = [platformContext](GroupNetworkState state) {
                tgvoip::jni::DoWithJNI([platformContext, state](JNIEnv *env) {
                    jobject globalRef = ((AndroidContext *) platformContext.get())->getJavaInstance();
                    env->CallVoidMethod(globalRef, env->GetMethodID(NativeInstanceClass, "onNetworkStateUpdated", "(ZZ)V"), state.isConnected, state.isTransitioningFromBroadcastToRtc);
                });
            },
            .audioLevelsUpdated = [platformContext](GroupLevelsUpdate const &update) {
                tgvoip::jni::DoWithJNI([platformContext, update](JNIEnv *env) {
                    unsigned int size = update.updates.size();
                    jintArray intArray = env->NewIntArray(size);
                    jfloatArray floatArray = env->NewFloatArray(size);
                    jbooleanArray boolArray = env->NewBooleanArray(size);

                    jint intFill[size];
                    jfloat floatFill[size];
                    jboolean boolFill[size];
                    for (int a = 0; a < size; a++) {
                        intFill[a] = update.updates[a].ssrc;
                        floatFill[a] = update.updates[a].value.isMuted ? 0 : update.updates[a].value.level;
                        boolFill[a] = !update.updates[a].value.isMuted && update.updates[a].value.voice;
                    }
                    env->SetIntArrayRegion(intArray, 0, size, intFill);
                    env->SetFloatArrayRegion(floatArray, 0, size, floatFill);
                    env->SetBooleanArrayRegion(boolArray, 0, size, boolFill);

                    jobject globalRef = ((AndroidContext *) platformContext.get())->getJavaInstance();
                    env->CallVoidMethod(globalRef, env->GetMethodID(NativeInstanceClass, "onAudioLevelsUpdated", "([I[F[Z)V"), intArray, floatArray, boolArray);
                    env->DeleteLocalRef(intArray);
                    env->DeleteLocalRef(floatArray);
                    env->DeleteLocalRef(boolArray);
                });
            },
            .videoCapture = videoCapture,
            .videoContentType = screencast ? VideoContentType::Screencast : VideoContentType::Generic,
            .initialEnableNoiseSuppression = (bool) noiseSupression,
            .platformContext = platformContext
    };
    if (!screencast) {
        descriptor.requestBroadcastPart = [](std::shared_ptr<PlatformContext> platformContext, int64_t timestamp, int64_t duration, std::function<void(BroadcastPart &&)> callback) -> std::shared_ptr<BroadcastPartTask> {
            std::shared_ptr<BroadcastPartTask> task = std::make_shared<BroadcastPartTaskJava>(platformContext, callback, timestamp);
            ((AndroidContext *) platformContext.get())->streamTask = task;
            tgvoip::jni::DoWithJNI([platformContext, timestamp, duration, task](JNIEnv *env) {
                jobject globalRef = ((AndroidContext *) platformContext.get())->getJavaInstance();
                env->CallVoidMethod(globalRef, env->GetMethodID(NativeInstanceClass, "onRequestBroadcastPart", "(JJ)V"), timestamp, duration);
            });
            return task;
        };
        descriptor.requestMediaChannelDescriptions = [platformContext](std::vector<uint32_t> const &ssrcs, std::function<void(std::vector<MediaChannelDescription> &&)> callback) -> std::shared_ptr<RequestMediaChannelDescriptionTask> {
            std::shared_ptr<RequestMediaChannelDescriptionTaskJava> task = std::make_shared<RequestMediaChannelDescriptionTaskJava>(platformContext, callback);
            ((AndroidContext *) platformContext.get())->descriptionTasks.push_back(task);
            tgvoip::jni::DoWithJNI([platformContext, ssrcs, task](JNIEnv *env) {
                unsigned int size = ssrcs.size();
                jintArray intArray = env->NewIntArray(size);

                jint intFill[size];
                for (int a = 0; a < size; a++) {
                    intFill[a] = ssrcs[a];
                }
                env->SetIntArrayRegion(intArray, 0, size, intFill);

                jobject globalRef = ((AndroidContext *) platformContext.get())->getJavaInstance();
                env->CallVoidMethod(globalRef, env->GetMethodID(NativeInstanceClass, "onParticipantDescriptionsRequired", "(J[I)V"), (jlong) task.get(), intArray);
                env->DeleteLocalRef(intArray);
            });
            return task;
        };
    }

    auto *holder = new InstanceHolder;
    holder->groupNativeInstance = std::make_unique<GroupInstanceCustomImpl>(std::move(descriptor));
    holder->_platformContext = platformContext;
    holder->_videoCapture = videoCapture;
    return reinterpret_cast<jlong>(holder);
}

JNIEXPORT void JNICALL Java_org_telegram_messenger_voip_NativeInstance_setJoinResponsePayload(JNIEnv *env, jobject obj, jstring payload) {
    InstanceHolder *instance = getInstanceHolder(env, obj);
    if (instance->groupNativeInstance == nullptr) {
        return;
    }
    instance->groupNativeInstance->setConnectionMode(GroupConnectionMode::GroupConnectionModeRtc, true);
    instance->groupNativeInstance->setJoinResponsePayload(tgvoip::jni::JavaStringToStdString(env, payload));
}

JNIEXPORT void JNICALL Java_org_telegram_messenger_voip_NativeInstance_prepareForStream(JNIEnv *env, jobject obj) {
    InstanceHolder *instance = getInstanceHolder(env, obj);
    if (instance->groupNativeInstance == nullptr) {
        return;
    }
    instance->groupNativeInstance->setConnectionMode(GroupConnectionMode::GroupConnectionModeBroadcast, true);
}

void onEmitJoinPayload(const std::shared_ptr<PlatformContext>& platformContext, const GroupJoinPayload& payload) {
    JNIEnv *env = webrtc::AttachCurrentThreadIfNeeded();
    jobject globalRef = ((AndroidContext *) platformContext.get())->getJavaInstance();
    env->CallVoidMethod(globalRef, env->GetMethodID(NativeInstanceClass, "onEmitJoinPayload", "(Ljava/lang/String;I)V"), env->NewStringUTF(payload.json.c_str()), (jint) payload.audioSsrc);
}

JNIEXPORT void JNICALL Java_org_telegram_messenger_voip_NativeInstance_resetGroupInstance(JNIEnv *env, jobject obj, jboolean set, jboolean disconnect) {
    InstanceHolder *instance = getInstanceHolder(env, obj);
    if (instance->groupNativeInstance == nullptr) {
        return;
    }
    if (set) {
        instance->groupNativeInstance->setConnectionMode(GroupConnectionMode::GroupConnectionModeNone, !disconnect);
    }
    std::shared_ptr<PlatformContext> platformContext = instance->_platformContext;
    instance->groupNativeInstance->emitJoinPayload([platformContext](const GroupJoinPayload& payload) {
        onEmitJoinPayload(platformContext, payload);
    });
}

void broadcastRequestedSinks(InstanceHolder *instance) {
    std::vector<VideoChannelDescription> descriptions;
    for (auto & remoteGroupSink : instance->remoteGroupSinks) {
        VideoChannelDescription description;
        description.endpointId = remoteGroupSink.second.endpointId;
        description.ssrcGroups = remoteGroupSink.second.ssrcGroups;
        description.maxQuality = remoteGroupSink.second.quality;
        descriptions.push_back(std::move(description));
    }
    instance->groupNativeInstance->setRequestedVideoChannels(std::move(descriptions));
}

JNIEXPORT void JNICALL Java_org_telegram_messenger_voip_NativeInstance_setNoiseSuppressionEnabled(JNIEnv *env, jobject obj, jboolean enabled) {
    InstanceHolder *instance = getInstanceHolder(env, obj);
    if (instance->groupNativeInstance == nullptr) {
        return;
    }
    instance->groupNativeInstance->setIsNoiseSuppressionEnabled(enabled);
}


JNIEXPORT jlong JNICALL Java_org_telegram_messenger_voip_NativeInstance_addIncomingVideoOutput(JNIEnv *env, jobject obj, jint quality, jstring endpointId, jobjectArray ssrcGroups, jobject remoteSink) {
    InstanceHolder *instance = getInstanceHolder(env, obj);
    if (instance->groupNativeInstance == nullptr) {
        return 0;
    }
    SetVideoSink sink;
    std::string endpointIdStr = tgvoip::jni::JavaStringToStdString(env, endpointId);
    std::shared_ptr<rtc::VideoSinkInterface<webrtc::VideoFrame>> ptr = webrtc::JavaToNativeVideoSink(env, remoteSink);
    sink.sink = ptr;
    sink.endpointId = endpointIdStr;
    if (ssrcGroups) {
        for (int i = 0, size = env->GetArrayLength(ssrcGroups); i < size; i++) {
            JavaObject javaObject(env, env->GetObjectArrayElement(ssrcGroups, i));
            MediaSsrcGroup ssrcGroup;
            ssrcGroup.semantics = tgvoip::jni::JavaStringToStdString(env, javaObject.getStringField("semantics"));
            jintArray ssrcsArray = javaObject.getIntArrayField("ssrcs");
            jint *elements = env->GetIntArrayElements(ssrcsArray, nullptr);
            for (int j = 0, size2 = env->GetArrayLength(ssrcsArray); j < size2; j++) {
                ssrcGroup.ssrcs.push_back(elements[j]);
            }
            env->ReleaseIntArrayElements(ssrcsArray, elements, JNI_ABORT);
            sink.ssrcGroups.push_back(std::move(ssrcGroup));
        }
    }
    sink.quality = (VideoChannelDescription::Quality) quality;
    instance->remoteGroupSinks[endpointIdStr] = std::move(sink);
    broadcastRequestedSinks(instance);
    instance->groupNativeInstance->addIncomingVideoOutput(endpointIdStr, ptr);
    return reinterpret_cast<intptr_t>(ptr.get());
}

JNIEXPORT void JNICALL Java_org_telegram_messenger_voip_NativeInstance_removeIncomingVideoOutput(JNIEnv *env, jobject obj, jlong nativeRemoteSink) {
    InstanceHolder *instance = getInstanceHolder(env, obj);
    if (instance->groupNativeInstance == nullptr) {
        return;
    }
    if (nativeRemoteSink == 0) {
        instance->remoteGroupSinks.clear();
    } else {
        for (auto iter = instance->remoteGroupSinks.begin(); iter != instance->remoteGroupSinks.end(); iter++) {
            if (reinterpret_cast<intptr_t>(iter->second.sink.get()) == nativeRemoteSink) {
                instance->remoteGroupSinks.erase(iter);
                break;
            }
        }
    }
    broadcastRequestedSinks(instance);
}

JNIEXPORT void JNICALL Java_org_telegram_messenger_voip_NativeInstance_setVideoEndpointQuality(JNIEnv *env, jobject obj, jstring endpointId, jint quality) {
    InstanceHolder *instance = getInstanceHolder(env, obj);
    if (instance->groupNativeInstance == nullptr) {
        return;
    }
    broadcastRequestedSinks(instance);
    auto sink = instance->remoteGroupSinks.find(tgvoip::jni::JavaStringToStdString(env, endpointId));
    if (sink == instance->remoteGroupSinks.end()) {
        return;
    }
    sink->second.quality = (VideoChannelDescription::Quality) quality;
    broadcastRequestedSinks(instance);
}

JNIEXPORT jlong JNICALL Java_org_telegram_messenger_voip_NativeInstance_makeNativeInstance(JNIEnv *env, jclass clazz, jstring version, jobject instanceObj, jobject config, jstring persistentStateFilePath, jobjectArray endpoints, jobject proxyClass, jint networkType, jobject encryptionKey, jobject remoteSink, jlong videoCapturer, jfloat aspectRatio) {
    initWebRTC(env);

    JavaObject configObject(env, config);
    JavaObject encryptionKeyObject(env, encryptionKey);
    std::string v = tgvoip::jni::JavaStringToStdString(env, version);

    jbyteArray valueByteArray = encryptionKeyObject.getByteArrayField("value");
    auto *valueBytes = (uint8_t *) env->GetByteArrayElements(valueByteArray, nullptr);
    auto encryptionKeyValue = std::make_shared<std::array<uint8_t, 256>>();
    memcpy(encryptionKeyValue->data(), valueBytes, 256);
    env->ReleaseByteArrayElements(valueByteArray, (jbyte *) valueBytes, JNI_ABORT);

    std::shared_ptr<VideoCaptureInterface> videoCapture = videoCapturer ? std::shared_ptr<VideoCaptureInterface>(reinterpret_cast<VideoCaptureInterface *>(videoCapturer)) : nullptr;

    std::shared_ptr<PlatformContext> platformContext;
    if (videoCapture) {
        platformContext = videoCapture->getPlatformContext();
        ((AndroidContext *) platformContext.get())->setJavaInstance(env, instanceObj);
    } else {
        platformContext = std::make_shared<AndroidContext>(env, instanceObj, false);
    }

    Descriptor descriptor = {
            .config = Config{
                    .initializationTimeout = configObject.getDoubleField("initializationTimeout"),
                    .receiveTimeout = configObject.getDoubleField("receiveTimeout"),
                    .dataSaving = parseDataSaving(env, configObject.getIntField("dataSaving")),
                    .enableP2P = configObject.getBooleanField("enableP2p") == JNI_TRUE,
                    .enableStunMarking = configObject.getBooleanField("enableSm") == JNI_TRUE,
                    .enableAEC = configObject.getBooleanField("enableAec") == JNI_TRUE,
                    .enableNS = configObject.getBooleanField("enableNs") == JNI_TRUE,
                    .enableAGC = configObject.getBooleanField("enableAgc") == JNI_TRUE,
                    .enableVolumeControl = true,
                    .logPath = {tgvoip::jni::JavaStringToStdString(env, configObject.getStringField("logPath"))},
                    .statsLogPath = {tgvoip::jni::JavaStringToStdString(env, configObject.getStringField("statsLogPath"))},
                    .maxApiLayer = configObject.getIntField("maxApiLayer"),
                    .enableHighBitrateVideo = true,
                    .preferredVideoCodecs = {cricket::kVp9CodecName}
            },
            .encryptionKey = EncryptionKey(
                    std::move(encryptionKeyValue),
                    encryptionKeyObject.getBooleanField("isOutgoing") == JNI_TRUE),
            .videoCapture =  videoCapture,
            .stateUpdated = [platformContext](State state) {
                jint javaState = asJavaState(state);
                jobject globalRef = ((AndroidContext *) platformContext.get())->getJavaInstance();
                tgvoip::jni::DoWithJNI([globalRef, javaState](JNIEnv *env) {
                    env->CallVoidMethod(globalRef, env->GetMethodID(NativeInstanceClass, "onStateUpdated", "(I)V"), javaState);
                });
            },
            .signalBarsUpdated = [platformContext](int count) {
                jobject globalRef = ((AndroidContext *) platformContext.get())->getJavaInstance();
                tgvoip::jni::DoWithJNI([globalRef, count](JNIEnv *env) {
                    env->CallVoidMethod(globalRef, env->GetMethodID(NativeInstanceClass, "onSignalBarsUpdated", "(I)V"), count);
                });
            },
            .audioLevelUpdated = [platformContext](float level) {
                tgvoip::jni::DoWithJNI([platformContext, level](JNIEnv *env) {
                    jintArray intArray = nullptr;
                    jfloatArray floatArray = env->NewFloatArray(1);
                    jbooleanArray boolArray = nullptr;

                    jfloat floatFill[1];
                    floatFill[0] = level;
                    env->SetFloatArrayRegion(floatArray, 0, 1, floatFill);

                    jobject globalRef = ((AndroidContext *) platformContext.get())->getJavaInstance();
                    env->CallVoidMethod(globalRef, env->GetMethodID(NativeInstanceClass, "onAudioLevelsUpdated", "([I[F[Z)V"), intArray, floatArray, boolArray);
                    env->DeleteLocalRef(floatArray);
                });
            },
            .remoteMediaStateUpdated = [platformContext](AudioState audioState, VideoState videoState) {
                jobject globalRef = ((AndroidContext *) platformContext.get())->getJavaInstance();
                tgvoip::jni::DoWithJNI([globalRef, audioState, videoState](JNIEnv *env) {
                    env->CallVoidMethod(globalRef, env->GetMethodID(NativeInstanceClass, "onRemoteMediaStateUpdated", "(II)V"), (jint) audioState, (jint )videoState);
                });
            },
            .signalingDataEmitted = [platformContext](const std::vector<uint8_t> &data) {
                jobject globalRef = ((AndroidContext *) platformContext.get())->getJavaInstance();
                tgvoip::jni::DoWithJNI([globalRef, data](JNIEnv *env) {
                    jbyteArray arr = copyVectorToJavaByteArray(env, data);
                    env->CallVoidMethod(globalRef, env->GetMethodID(NativeInstanceClass, "onSignalingData", "([B)V"), arr);
                    env->DeleteLocalRef(arr);
                });
            },
            .platformContext = platformContext,
    };

    for (int i = 0, size = env->GetArrayLength(endpoints); i < size; i++) {
        JavaObject endpointObject(env, env->GetObjectArrayElement(endpoints, i));
        bool isRtc = endpointObject.getBooleanField("isRtc");
        if (isRtc) {
            RtcServer rtcServer;
            rtcServer.host = tgvoip::jni::JavaStringToStdString(env, endpointObject.getStringField("ipv4"));
            rtcServer.port = static_cast<uint16_t>(endpointObject.getIntField("port"));
            rtcServer.login = tgvoip::jni::JavaStringToStdString(env, endpointObject.getStringField("username"));
            rtcServer.password = tgvoip::jni::JavaStringToStdString(env, endpointObject.getStringField("password"));
            rtcServer.isTurn = endpointObject.getBooleanField("turn");
            descriptor.rtcServers.push_back(std::move(rtcServer));
        } else {
            Endpoint endpoint;
            endpoint.endpointId = endpointObject.getLongField("id");
            endpoint.host = EndpointHost{tgvoip::jni::JavaStringToStdString(env, endpointObject.getStringField("ipv4")), tgvoip::jni::JavaStringToStdString(env, endpointObject.getStringField("ipv6"))};
            endpoint.port = static_cast<uint16_t>(endpointObject.getIntField("port"));
            endpoint.type = parseEndpointType(env, endpointObject.getIntField("type"));
            jbyteArray peerTag = endpointObject.getByteArrayField("peerTag");
            if (peerTag && env->GetArrayLength(peerTag)) {
                jbyte *peerTagBytes = env->GetByteArrayElements(peerTag, nullptr);
                memcpy(endpoint.peerTag, peerTagBytes, 16);
                env->ReleaseByteArrayElements(peerTag, peerTagBytes, JNI_ABORT);
            }
            descriptor.endpoints.push_back(std::move(endpoint));
        }
    }

    if (!env->IsSameObject(proxyClass, nullptr)) {
        JavaObject proxyObject(env, proxyClass);
        descriptor.proxy = std::make_unique<Proxy>();
        descriptor.proxy->host = tgvoip::jni::JavaStringToStdString(env, proxyObject.getStringField("host"));
        descriptor.proxy->port = static_cast<uint16_t>(proxyObject.getIntField("port"));
        descriptor.proxy->login = tgvoip::jni::JavaStringToStdString(env, proxyObject.getStringField("login"));
        descriptor.proxy->password = tgvoip::jni::JavaStringToStdString(env, proxyObject.getStringField("password"));
    }

    readPersistentState(tgvoip::jni::JavaStringToStdString(env, persistentStateFilePath).c_str(), descriptor.persistentState);

    auto *holder = new InstanceHolder;
    holder->nativeInstance = tgcalls::Meta::Create(v, std::move(descriptor));
    holder->_videoCapture = videoCapture;
    holder->_platformContext = platformContext;
    holder->nativeInstance->setIncomingVideoOutput(webrtc::JavaToNativeVideoSink(env, remoteSink));
    holder->nativeInstance->setNetworkType(parseNetworkType(networkType));
    holder->nativeInstance->setRequestedVideoAspect(aspectRatio);
    return reinterpret_cast<jlong>(holder);
}

JNIEXPORT void JNICALL Java_org_telegram_messenger_voip_NativeInstance_setGlobalServerConfig(JNIEnv *env, jobject obj, jstring serverConfigJson) {
    SetLegacyGlobalServerConfig(tgvoip::jni::JavaStringToStdString(env, serverConfigJson));
}

JNIEXPORT jstring JNICALL Java_org_telegram_messenger_voip_NativeInstance_getVersion(JNIEnv *env, jobject obj) {
    return env->NewStringUTF(tgvoip::VoIPController::GetVersion());
}

JNIEXPORT void JNICALL Java_org_telegram_messenger_voip_NativeInstance_setBufferSize(JNIEnv *env, jobject obj, jint size) {
    tgvoip::audio::AudioOutputOpenSLES::nativeBufferSize = (unsigned int) size;
    tgvoip::audio::AudioInputOpenSLES::nativeBufferSize = (unsigned int) size;
}

JNIEXPORT void JNICALL Java_org_telegram_messenger_voip_NativeInstance_setNetworkType(JNIEnv *env, jobject obj, jint networkType) {
    InstanceHolder *instance = getInstanceHolder(env, obj);
    if (instance->nativeInstance == nullptr) {
        return;
    }
    instance->nativeInstance->setNetworkType(parseNetworkType(networkType));
}

JNIEXPORT void JNICALL Java_org_telegram_messenger_voip_NativeInstance_setMuteMicrophone(JNIEnv *env, jobject obj, jboolean muteMicrophone) {
    InstanceHolder *instance = getInstanceHolder(env, obj);
    if (instance->nativeInstance != nullptr) {
        instance->nativeInstance->setMuteMicrophone(muteMicrophone);
    } else if (instance->groupNativeInstance != nullptr) {
        instance->groupNativeInstance->setIsMuted(muteMicrophone);
    }
}

JNIEXPORT void JNICALL Java_org_telegram_messenger_voip_NativeInstance_setVolume(JNIEnv *env, jobject obj, jint ssrc, jdouble volume) {
    InstanceHolder *instance = getInstanceHolder(env, obj);
    if (instance->groupNativeInstance != nullptr) {
        instance->groupNativeInstance->setVolume(ssrc, volume);
    }
}

JNIEXPORT void JNICALL Java_org_telegram_messenger_voip_NativeInstance_setAudioOutputGainControlEnabled(JNIEnv *env, jobject obj, jboolean enabled) {
    InstanceHolder *instance = getInstanceHolder(env, obj);
    if (instance->nativeInstance == nullptr) {
        return;
    }
    instance->nativeInstance->setAudioOutputGainControlEnabled(enabled);
}

JNIEXPORT void JNICALL Java_org_telegram_messenger_voip_NativeInstance_setEchoCancellationStrength(JNIEnv *env, jobject obj, jint strength) {
    InstanceHolder *instance = getInstanceHolder(env, obj);
    if (instance->nativeInstance == nullptr) {
        return;
    }
    instance->nativeInstance->setEchoCancellationStrength(strength);
}

JNIEXPORT jstring JNICALL Java_org_telegram_messenger_voip_NativeInstance_getLastError(JNIEnv *env, jobject obj) {
    InstanceHolder *instance = getInstanceHolder(env, obj);
    if (instance->nativeInstance == nullptr) {
        return nullptr;
    }
    return env->NewStringUTF(instance->nativeInstance->getLastError().c_str());
}

JNIEXPORT jstring JNICALL Java_org_telegram_messenger_voip_NativeInstance_getDebugInfo(JNIEnv *env, jobject obj) {
    InstanceHolder *instance = getInstanceHolder(env, obj);
    if (instance->nativeInstance == nullptr) {
        return nullptr;
    }
    return env->NewStringUTF(instance->nativeInstance->getDebugInfo().c_str());
}

JNIEXPORT jlong JNICALL Java_org_telegram_messenger_voip_NativeInstance_getPreferredRelayId(JNIEnv *env, jobject obj) {
    InstanceHolder *instance = getInstanceHolder(env, obj);
    if (instance->nativeInstance == nullptr) {
        return 0;
    }
    return instance->nativeInstance->getPreferredRelayId();
}

JNIEXPORT jobject JNICALL Java_org_telegram_messenger_voip_NativeInstance_getTrafficStats(JNIEnv *env, jobject obj) {
    InstanceHolder *instance = getInstanceHolder(env, obj);
    if (instance->nativeInstance == nullptr) {
        return nullptr;
    }
    return asJavaTrafficStats(env, instance->nativeInstance->getTrafficStats());
}

JNIEXPORT jbyteArray JNICALL Java_org_telegram_messenger_voip_NativeInstance_getPersistentState(JNIEnv *env, jobject obj) {
    InstanceHolder *instance = getInstanceHolder(env, obj);
    if (instance->nativeInstance == nullptr) {
        return nullptr;
    }
    return copyVectorToJavaByteArray(env, instance->nativeInstance->getPersistentState().value);
}

JNIEXPORT void JNICALL Java_org_telegram_messenger_voip_NativeInstance_stopNative(JNIEnv *env, jobject obj) {
    InstanceHolder *instance = getInstanceHolder(env, obj);
    if (instance->nativeInstance == nullptr) {
        return;
    }
    instance->nativeInstance->stop([instance](const FinalState& finalState) {
        JNIEnv *env = webrtc::AttachCurrentThreadIfNeeded();
        jobject globalRef = ((AndroidContext *) instance->_platformContext.get())->getJavaInstance();
        const std::string &path = tgvoip::jni::JavaStringToStdString(env, JavaObject(env, globalRef).getStringField("persistentStateFilePath"));
        savePersistentState(path.c_str(), finalState.persistentState);
        env->CallVoidMethod(globalRef, env->GetMethodID(NativeInstanceClass, "onStop", "(Lorg/telegram/messenger/voip/Instance$FinalState;)V"), asJavaFinalState(env, finalState));
        delete instance;
    });
}

JNIEXPORT void JNICALL Java_org_telegram_messenger_voip_NativeInstance_stopGroupNative(JNIEnv *env, jobject obj) {
    InstanceHolder *instance = getInstanceHolder(env, obj);
    if (instance->groupNativeInstance == nullptr) {
        return;
    }
    instance->groupNativeInstance->stop();
    instance->groupNativeInstance.reset();
    delete instance;
}

JNIEXPORT void JNICALL Java_org_telegram_messenger_voip_NativeInstance_onStreamPartAvailable(JNIEnv *env, jobject obj, jlong ts, jobject byteBuffer, jint size, jlong responseTs) {
    InstanceHolder *instance = getInstanceHolder(env, obj);
    if (instance->groupNativeInstance == nullptr) {
        return;
    }
    auto context = (AndroidContext *) instance->_platformContext.get();
    std::shared_ptr<BroadcastPartTask> streamTask = context->streamTask;
    auto task = (BroadcastPartTaskJava *) streamTask.get();
    if (task != nullptr) {
        if (byteBuffer != nullptr) {
            auto buf = (uint8_t *) env->GetDirectBufferAddress(byteBuffer);
            task->call(ts, responseTs, BroadcastPart::Status::Success, buf, size);
        } else {
            task->call(ts, responseTs, size == 0 ? BroadcastPart::Status::NotReady : BroadcastPart::Status::ResyncNeeded, nullptr, 0);
        }
    }
}

JNIEXPORT void JNICALL Java_org_telegram_messenger_voip_NativeInstance_onMediaDescriptionAvailable(JNIEnv *env, jobject obj, jlong taskPtr, jintArray ssrcs) {
    InstanceHolder *instance = getInstanceHolder(env, obj);
    if (instance->groupNativeInstance == nullptr) {
        return;
    }
    auto task = reinterpret_cast<RequestMediaChannelDescriptionTaskJava *>(taskPtr);
    task->call(env, ssrcs);
    auto context = (AndroidContext *) instance->_platformContext.get();
    for (auto iter = context->descriptionTasks.begin(); iter != context->descriptionTasks.end(); iter++) {
        if (reinterpret_cast<intptr_t>(iter->get()) == taskPtr) {
            context->descriptionTasks.erase(iter);
            break;
        }
    }
}

JNIEXPORT jlong JNICALL Java_org_telegram_messenger_voip_NativeInstance_createVideoCapturer(JNIEnv *env, jclass clazz, jobject localSink, jint type) {
    initWebRTC(env);
    std::unique_ptr<VideoCaptureInterface> capture;
    if (type == 0 || type == 1) {
        capture = tgcalls::VideoCaptureInterface::Create(StaticThreads::getThreads(), type == 1 ? "front" : "back", false, std::make_shared<AndroidContext>(env, nullptr, false));
    } else {
        capture = tgcalls::VideoCaptureInterface::Create(StaticThreads::getThreads(), "screen", true, std::make_shared<AndroidContext>(env, nullptr, true));
    }
    capture->setOutput(webrtc::JavaToNativeVideoSink(env, localSink));
    capture->setState(VideoState::Active);
    return reinterpret_cast<intptr_t>(capture.release());
}

JNIEXPORT void JNICALL Java_org_telegram_messenger_voip_NativeInstance_activateVideoCapturer(JNIEnv *env, jobject obj, jlong videoCapturer) {
    InstanceHolder *instance = getInstanceHolder(env, obj);
    if (instance->nativeInstance) {
        instance->nativeInstance->setVideoCapture(nullptr);
    } else if (instance->groupNativeInstance) {
        instance->groupNativeInstance->setVideoSource(nullptr);
    }
    auto capturer = reinterpret_cast<VideoCaptureInterface *>(videoCapturer);
    capturer->setState(VideoState::Active);
}

JNIEXPORT void JNICALL Java_org_telegram_messenger_voip_NativeInstance_clearVideoCapturer(JNIEnv *env, jobject obj) {
    InstanceHolder *instance = getInstanceHolder(env, obj);
    if (instance->nativeInstance) {
        instance->nativeInstance->setVideoCapture(nullptr);
    } else if (instance->groupNativeInstance) {
        instance->groupNativeInstance->setVideoSource(nullptr);
    }
}

JNIEXPORT void JNICALL Java_org_telegram_messenger_voip_NativeInstance_destroyVideoCapturer(JNIEnv *env, jclass clazz, jlong videoCapturer) {
    auto capturer = reinterpret_cast<VideoCaptureInterface *>(videoCapturer);
    delete capturer;
}

JNIEXPORT void JNICALL Java_org_telegram_messenger_voip_NativeInstance_switchCameraCapturer(JNIEnv *env, jclass clazz, jlong videoCapturer, jboolean front) {
    auto capturer = reinterpret_cast<VideoCaptureInterface *>(videoCapturer);
    capturer->switchToDevice(front ? "front" : "back");
}

JNIEXPORT void JNICALL Java_org_telegram_messenger_voip_NativeInstance_setVideoStateCapturer(JNIEnv *env, jclass clazz, jlong videoCapturer, jint videoState) {
    auto capturer = reinterpret_cast<VideoCaptureInterface *>(videoCapturer);
    capturer->setState(static_cast<VideoState>(videoState));
}

JNIEXPORT void JNICALL Java_org_telegram_messenger_voip_NativeInstance_switchCamera(JNIEnv *env, jobject obj, jboolean front) {
    InstanceHolder *instance = getInstanceHolder(env, obj);
    if (instance->_videoCapture == nullptr) {
        return;
    }
    instance->_videoCapture->switchToDevice(front ? "front" : "back");
}

JNIEXPORT jboolean JNICALL Java_org_telegram_messenger_voip_NativeInstance_hasVideoCapturer(JNIEnv *env, jobject obj) {
    InstanceHolder *instance = getInstanceHolder(env, obj);
    if (instance->_videoCapture == nullptr) {
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

JNIEXPORT void Java_org_telegram_messenger_voip_NativeInstance_setVideoState(JNIEnv *env, jobject obj, jint state) {
    InstanceHolder *instance = getInstanceHolder(env, obj);
    std::shared_ptr<tgcalls::VideoCaptureInterface> capturer = instance->useScreencast ? instance->_screenVideoCapture : instance->_videoCapture;
    if (capturer == nullptr) {
        return;
    }
    capturer->setState(static_cast<VideoState>(state));
}

JNIEXPORT void JNICALL Java_org_telegram_messenger_voip_NativeInstance_setupOutgoingVideo(JNIEnv *env, jobject obj, jobject localSink, jint type) {
    InstanceHolder *instance = getInstanceHolder(env, obj);
    std::shared_ptr<tgcalls::VideoCaptureInterface> capturer;
    if (type == 0 || type == 1) {
        if (instance->_videoCapture == nullptr) {
            instance->_videoCapture = tgcalls::VideoCaptureInterface::Create(StaticThreads::getThreads(), type == 1 ? "front" : "back", false, std::make_shared<AndroidContext>(env, nullptr, false));
        }
        capturer = instance->_videoCapture;
        instance->useScreencast = false;
    } else {
        if (instance->_screenVideoCapture == nullptr) {
            instance->_screenVideoCapture = tgcalls::VideoCaptureInterface::Create(StaticThreads::getThreads(), "screen", true, std::make_shared<AndroidContext>(env, nullptr, true));
        }
        capturer = instance->_screenVideoCapture;
        instance->useScreencast = true;
    }
    capturer->setOutput(webrtc::JavaToNativeVideoSink(env, localSink));
    capturer->setState(VideoState::Active);
    if (instance->nativeInstance) {
        instance->nativeInstance->setVideoCapture(capturer);
    } else if (instance->groupNativeInstance) {
        instance->groupNativeInstance->setVideoCapture(capturer);
    }
}

JNIEXPORT void JNICALL Java_org_telegram_messenger_voip_NativeInstance_setupOutgoingVideoCreated(JNIEnv *env, jobject obj, jlong videoCapturer) {
    if (videoCapturer == 0) {
        return;
    }
    InstanceHolder *instance = getInstanceHolder(env, obj);
    if (instance->_videoCapture == nullptr) {
        instance->_videoCapture = std::shared_ptr<VideoCaptureInterface>(reinterpret_cast<VideoCaptureInterface *>(videoCapturer));
    }
    instance->_videoCapture->setState(VideoState::Active);
    if (instance->nativeInstance) {
        instance->nativeInstance->setVideoCapture(instance->_videoCapture);
        instance->useScreencast = false;
    } else if (instance->groupNativeInstance) {
        instance->groupNativeInstance->setVideoCapture(instance->_videoCapture);
    }
}

JNIEXPORT void JNICALL Java_org_telegram_messenger_voip_NativeInstance_onSignalingDataReceive(JNIEnv *env, jobject obj, jbyteArray value) {
    InstanceHolder *instance = getInstanceHolder(env, obj);
    if (instance->nativeInstance == nullptr) {
        return;
    }

    auto *valueBytes = (uint8_t *) env->GetByteArrayElements(value, nullptr);
    const size_t size = env->GetArrayLength(value);
    auto array = std::vector<uint8_t>(size);
    memcpy(&array[0], valueBytes, size);
    instance->nativeInstance->receiveSignalingData(array);
    env->ReleaseByteArrayElements(value, (jbyte *) valueBytes, JNI_ABORT);
}

}