package ru.mvideo.assystnotifier;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.Advapi32;
import com.sun.jna.platform.win32.W32Errors;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinReg;
import com.sun.jna.ptr.IntByReference;

import java.util.Objects;

class RegistryHelper {
    static String getStrRegKey(WinReg.HKEY rootKey, String regKey, String keyName) {
        //создаем основной указатель типа HKEY (хендлер) на ключ реестра,
        // с которым будем работать дальше
        WinReg.HKEYByReference phKey = new WinReg.HKEYByReference();
        //открываем доступ к разделу HKCU - HKEY_CURRENT_USER
        // в данном методе (для простоты) работаем только с этим разделом
        if (W32Errors.ERROR_SUCCESS == Advapi32.INSTANCE.RegOpenKeyEx(
                rootKey,
                regKey,
                0,
                WinNT.KEY_READ,
                phKey)) {
            //если доступ в раздел получен - получен и хендлер к раздела - phKey

            //заводим парочку необходимых указателей на данные, которые должны получить,
            //обратившись к следующей функции API
            IntByReference lpcbData = new IntByReference();
            IntByReference lpType = new IntByReference();
            //вызываем функцию получения данных по ключу реестра из открытого раздела,
            //но пока только с целью получения информации о наличии этих данных и о
            //типе данных, хранимых там
            if (W32Errors.ERROR_SUCCESS == Advapi32.INSTANCE.RegQueryValueEx(
                    phKey.getValue(),
                    keyName,
                    0,
                    lpType,
                    (char[]) null,
                    lpcbData)
                    ) {
                //если данные есть и тип их строковый - на этот раз запрашиваются
                //реальные данные, которые будут получены в символьном буфере, который
                //резервирует место в памяти под реальный полученный размер информации
                if ((WinNT.REG_SZ == lpType.getValue()) && ((lpcbData.getValue() > 0))) {
                    char[] buffer = new char[lpcbData.getValue()];
                    if (W32Errors.ERROR_SUCCESS == Advapi32.INSTANCE.RegQueryValueEx(
                            phKey.getValue(),
                            keyName,
                            0,
                            lpType,
                            buffer,
                            lpcbData)
                            ) {
                        //если данные благополучно получены - закрываем ключ реестра
                        Advapi32.INSTANCE.RegCloseKey(phKey.getValue());
                        //в приемном буфере лежит нуль-строка, её надо превратить в
                        //"нормальную строку";
                        return Native.toString(buffer);
                    }
                }

            }
        }
        return "";
    }

    static boolean putStrRegKey(WinReg.HKEY rootKey, String regKey, String keyName, String strValue) {
        //создаем основной указатель типа HKEY (хендлер) на ключ реестра,
        // с которым будем работать дальше
        WinReg.HKEYByReference phKey = new WinReg.HKEYByReference();
        //открываем доступ к разделу HKCU - HKEY_CURRENT_USER
        // в данном методе (для простоты) работаем только с этим разделом
        if (W32Errors.ERROR_SUCCESS == Advapi32.INSTANCE.RegOpenKeyEx(
                rootKey,
                regKey,
                0,
                WinNT.KEY_WRITE | WinNT.KEY_READ,
                phKey)) {
            //если доступ в раздел получен - получен и хендлер к раздела - phKey
            //записываем пару ключ - REG_SZ значение
            char[] lpData = Native.toCharArray(strValue);
            if (W32Errors.ERROR_SUCCESS == Advapi32.INSTANCE.RegSetValueEx(
                    phKey.getValue(),
                    keyName,
                    0,
                    WinNT.REG_SZ,
                    lpData,
                    lpData.length * 2)) {
                //для проверки перечитаем записанное
                IntByReference lpcbData = new IntByReference();
                IntByReference lpType = new IntByReference();
                //вызываем функцию получения данных по ключу реестра из открытого раздела,
                //но пока только с целью получения информации о наличии этих данных и о
                //типе данных, хранимых там
                if (W32Errors.ERROR_SUCCESS == Advapi32.INSTANCE.RegQueryValueEx(
                        phKey.getValue(),
                        keyName,
                        0,
                        lpType,
                        (char[]) null,
                        lpcbData)
                        ) {
                    //если данные есть и тип их строковый - на этот раз запрашиваются
                    //реальные данные, которые будут получены в символьном буфере, который
                    //резервирует место в памяти под реальный полученный размер информации
                    if ((WinNT.REG_SZ == lpType.getValue()) && ((lpcbData.getValue() > 0))) {
                        char[] buffer = new char[lpcbData.getValue()];
                        if (W32Errors.ERROR_SUCCESS == Advapi32.INSTANCE.RegQueryValueEx(
                                phKey.getValue(),
                                keyName,
                                0,
                                lpType,
                                buffer,
                                lpcbData)
                                ) {
                            //если данные благополучно получены - закрываем ключ реестра
                            Advapi32.INSTANCE.RegCloseKey(phKey.getValue());
                            //в приемном буфере лежит нуль-строка, её надо превратить в
                            //"нормальную строку"
                            String iStr = Native.toString(buffer);
                            //проверяем записанное и прочитанное
                            return (Objects.equals(iStr, strValue));
                        }
                    }
                }
            }
        }
        return false;
    }
}
