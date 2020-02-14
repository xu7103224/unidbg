package com.github.unidbg.ios.service;

import com.github.unidbg.Emulator;
import com.github.unidbg.Symbol;
import com.github.unidbg.arm.HookStatus;
import com.github.unidbg.arm.context.RegisterContext;
import com.github.unidbg.hook.ReplaceCallback;
import com.github.unidbg.hook.substrate.ISubstrate;
import com.github.unidbg.ios.Substrate;
import com.github.unidbg.ios.objc.Constants;
import com.github.unidbg.ios.objc.ObjC;
import com.github.unidbg.ios.struct.objc.ObjcClass;
import com.github.unidbg.ios.struct.objc.ObjcObject;
import com.sun.jna.Pointer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.nio.charset.StandardCharsets;

public class CTTelephonyNetworkInfo extends ServiceHook implements Constants {

    private static final Log log = LogFactory.getLog(CTTelephonyNetworkInfo.class);

    private final Symbol __CTServerConnectionCopyProviderNameUsingCarrierBundle;
    private final Symbol __CTServerConnectionCopyMobileSubscriberAndIsoCountryCodes;
    private final Symbol __CTServerConnectionCopyMobileSubscriberNetworkCode;
    private final Symbol __CTServerConnectionCarrierSettingsCopyValue;

    public CTTelephonyNetworkInfo(Emulator emulator) {
        super(emulator, "CoreTelephony");

        __CTServerConnectionCopyProviderNameUsingCarrierBundle = module.findSymbolByName("__CTServerConnectionCopyProviderNameUsingCarrierBundle", false);
        if (__CTServerConnectionCopyProviderNameUsingCarrierBundle == null) {
            throw new IllegalStateException("__CTServerConnectionCopyProviderNameUsingCarrierBundle is null");
        }

        __CTServerConnectionCopyMobileSubscriberAndIsoCountryCodes = module.findSymbolByName("__CTServerConnectionCopyMobileSubscriberAndIsoCountryCodes", false);
        if (__CTServerConnectionCopyMobileSubscriberAndIsoCountryCodes == null) {
            throw new IllegalStateException("__CTServerConnectionCopyMobileSubscriberAndIsoCountryCodes is null");
        }

        __CTServerConnectionCopyMobileSubscriberNetworkCode = module.findSymbolByName("__CTServerConnectionCopyMobileSubscriberNetworkCode", false);
        if (__CTServerConnectionCopyMobileSubscriberNetworkCode == null) {
            throw new IllegalStateException("__CTServerConnectionCopyMobileSubscriberNetworkCode is null");
        }

        __CTServerConnectionCarrierSettingsCopyValue = module.findSymbolByName("__CTServerConnectionCarrierSettingsCopyValue", false);
        if (__CTServerConnectionCarrierSettingsCopyValue == null) {
            throw new IllegalStateException("__CTServerConnectionCarrierSettingsCopyValue is null");
        }
    }

    @Override
    public void tryHook() {
        ObjC objc = ObjC.getInstance(emulator);
        ObjcClass cCTTelephonyNetworkInfo = objc.getClass("CTTelephonyNetworkInfo");
        ObjcClass cNSString = objc.getClass("NSString");
        ObjcClass cNSNumber = objc.getClass("NSNumber");
        final ObjcObject fakeCarrierName = cNSString.callObjc("stringWithCString:encoding:", "中国联通", NSUTF8StringEncoding);
        final ObjcObject fakeCountryCode = cNSString.callObjc("stringWithCString:", "460");
        final ObjcObject fakeIsoCountryCode = cNSString.callObjc("stringWithCString:", "cn");
        final ObjcObject fakeMobileNetworkCode = cNSString.callObjc("stringWithCString:", "01");
        final ObjcObject fakeAllowsVoIP = cNSNumber.callObjc("numberWithBool:", YES);

        ISubstrate substrate = Substrate.getInstance(emulator);
        substrate.hookFunction(__CTServerConnectionCopyProviderNameUsingCarrierBundle, new ReplaceCallback() {
            @Override
            public HookStatus onCall(Emulator emulator, long originFunction) {
                RegisterContext context = emulator.getContext();
                Pointer connection = context.getPointerArg(0);
                Pointer carrierName = context.getPointerArg(1);
                if (log.isDebugEnabled()) {
                    log.debug("__CTServerConnectionCopyProviderNameUsingCarrierBundle connection=" + connection + ", carrierName=" + carrierName);
                }
                carrierName.setPointer(0, fakeCarrierName.getPointer());
                return HookStatus.LR(emulator, 0);
            }
        });
        substrate.hookFunction(__CTServerConnectionCopyMobileSubscriberAndIsoCountryCodes, new ReplaceCallback() {
            @Override
            public HookStatus onCall(Emulator emulator, long originFunction) {
                RegisterContext context = emulator.getContext();
                Pointer connection = context.getPointerArg(0);
                Pointer countryCode = context.getPointerArg(1);
                Pointer isoCountryCode = context.getPointerArg(2);
                if (log.isDebugEnabled()) {
                    log.debug("__CTServerConnectionCopyMobileSubscriberAndIsoCountryCodes connection=" + connection + ", countryCode=" + countryCode + ", isoCountryCode=" + isoCountryCode);
                }
                countryCode.setPointer(0, fakeCountryCode.getPointer());
                isoCountryCode.setPointer(0, fakeIsoCountryCode.getPointer());
                return HookStatus.LR(emulator, 0);
            }
        });
        substrate.hookFunction(__CTServerConnectionCopyMobileSubscriberNetworkCode, new ReplaceCallback() {
            @Override
            public HookStatus onCall(Emulator emulator, long originFunction) {
                RegisterContext context = emulator.getContext();
                Pointer connection = context.getPointerArg(0);
                Pointer mobileNetworkCode = context.getPointerArg(1);
                if (log.isDebugEnabled()) {
                    log.debug("__CTServerConnectionCopyMobileSubscriberNetworkCode connection=" + connection + ", mobileNetworkCode=" + mobileNetworkCode);
                }
                mobileNetworkCode.setPointer(0, fakeMobileNetworkCode.getPointer());
                return HookStatus.LR(emulator, 0);
            }
        });
        substrate.hookFunction(__CTServerConnectionCarrierSettingsCopyValue, new ReplaceCallback() {
            @Override
            public HookStatus onCall(Emulator emulator, long originFunction) {
                RegisterContext context = emulator.getContext();
                Pointer connection = context.getPointerArg(0);
                Pointer key = context.getPointerArg(1);
                Pointer value = context.getPointerArg(2);
                int key_length = key.share(emulator.getPointerSize() * 3).getInt(0);
                byte[] key_data = key.share(emulator.getPointerSize() * 2).getPointer(0).getByteArray(0, key_length);
                String keyName = new String(key_data, StandardCharsets.UTF_8);
                if (log.isDebugEnabled()) {
                    log.debug("__CTServerConnectionCarrierSettingsCopyValue connection=" + connection + ", key=" + key + ", value=" + value + ", keyName=" + keyName);
                }
                if ("AllowsVoIP".equals(keyName)) {
                    value.setPointer(0, fakeAllowsVoIP.getPointer());
                    return HookStatus.LR(emulator, 0);
                }
                return HookStatus.RET(emulator, originFunction);
            }
        });
        substrate.hookMessageEx(cCTTelephonyNetworkInfo, objc.registerName("queryDataMode"), new ReplaceCallback() { // updateRadioAccessTechnology
            @Override
            public HookStatus onCall(Emulator emulator, long originFunction) {
                RegisterContext context = emulator.getContext();
                Pointer self = context.getPointerArg(0);
                Pointer selector = context.getPointerArg(1);
                if (log.isDebugEnabled()) {
                    log.debug("[CTTelephonyNetworkInfo queryDataMode] self=" + self + ", selector=" + selector);
                }
                return HookStatus.LR(emulator, 0);
            }
        });
        substrate.hookMessageEx(cCTTelephonyNetworkInfo, objc.registerName("queryCTSignalStrengthNotification"), new ReplaceCallback() { // updateSignalStrength
            @Override
            public HookStatus onCall(Emulator emulator, long originFunction) {
                RegisterContext context = emulator.getContext();
                Pointer self = context.getPointerArg(0);
                Pointer selector = context.getPointerArg(1);
                if (log.isDebugEnabled()) {
                    log.debug("[CTTelephonyNetworkInfo queryCTSignalStrengthNotification] self=" + self + ", selector=" + selector);
                }
                return HookStatus.LR(emulator, 0);
            }
        });
    }

}