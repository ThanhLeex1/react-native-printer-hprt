/* eslint-disable no-bitwise */
import { NativeEventEmitter, NativeModules, Platform } from 'react-native';
import {
  PrinterInterface,
  type HprtPrinterInfo,
  type HprtPrinterType,
  type OptionPrinter,
  type PrinterImage,
  type PrinterStatus,
  type StatusDrawer,
} from './typing';

export type {
  HprtPrinterInfo,
  HprtPrinterType,
  OptionPrinter,
  PrinterImage,
  PrinterInterface,
  PrinterStatus,
  StatusDrawer,
};
const VENDER_ID_TP808 = 8401;
const LINKING_ERROR =
  `The package 'hprt-printer' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go\n';

const HprtPrinter = NativeModules.HprtPrinterModule
  ? NativeModules.HprtPrinterModule
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );
const NetPrinter = NativeModules.NetPrintHPRTModule
  ? NativeModules.NetPrintHPRTModule
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );
const UsbPrinter = NativeModules.UsbPrintHPRTModule
  ? NativeModules.UsbPrintHPRTModule
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );

const PrinterHprt: HprtPrinterType = {
  async discoveryDevices(
    interfacePrinter: PrinterInterface,
    onPrinterFound,
    onFinished
  ) {
    try {
      let totalPrinters = 0;
      let processedPrinters = 0;

      //listner device granted permession
      const eventEmitter = new NativeEventEmitter(
        NativeModules.HprtPrinterModule
      );
      const eventListener = eventEmitter.addListener(
        'KEY_UPDATE_LIST_DEVICE',
        (printer: HprtPrinterInfo) => {
          processedPrinters++;
          onPrinterFound && onPrinterFound(printer);
          if (processedPrinters >= totalPrinters) {
            onFinished();
            eventListener.remove();
          }
        }
      );

      const result: HprtPrinterInfo[] =
        interfacePrinter === PrinterInterface.USB
          ? await UsbPrinter.getDevices()
          : await NetPrinter.getDevices();

      let printers = result.filter((item) => item.vendorId === VENDER_ID_TP808);
      totalPrinters = printers.length;
      if (interfacePrinter === PrinterInterface.USB && totalPrinters > 0) {
        for (const printer of printers) {
          let hasPermission = await UsbPrinter.hasPermissionUSB(
            printer.vendorId,
            printer.productId
          );
          if (!hasPermission) {
            UsbPrinter.requestPermissionUSB(
              printer.vendorId,
              printer.productId
            );
          } else {
            onPrinterFound && onPrinterFound(printer);
            processedPrinters++;
          }
        }
      } else {
        for (const printer of printers) {
          onPrinterFound(printer);
        }
      }
    } catch (error) {
      console.error('Error fetching devices:', error);
      throw error;
    }
  },
  async onConnect(printer: HprtPrinterInfo) {
    if (!printer) {
      throw 'Not found printer';
    }
    try {
      if (printer?.interfacePrinter === PrinterInterface.USB) {
        const hasPermission = await UsbPrinter.hasPermissionUSB(
          printer?.vendorId,
          printer?.productId
        );
        if (!hasPermission) {
          return await UsbPrinter.requestPermissionUSB(
            printer?.vendorId,
            printer?.productId
          );
        }
      }

      return await HprtPrinter.connectDevice(printer);
    } catch (error) {
      console.error('Error onConnect:', error);
      throw error;
    }
  },
  async onDisConnect() {
    return HprtPrinter.disConnectDevice();
  },
  async printImage(request: PrinterImage) {
    try {
      const res = await HprtPrinter.printImage({
        source: request.source,
        isBase64: true,
        isRotate: false,
        isLzo: false,
        size: 576,
        ...(request.options || {}),
      });
      return res;
    } catch (error) {
      console.error('Failed to print image:', error);
      throw error;
    }
  },
  cutPaper() {
    return HprtPrinter.cutPaper({ cutMode: 1, distance: 0 });
  },
  async getCashDrawer() {
    try {
      const status = await HprtPrinter.getCashDrawer();
      return { isOpen: status === 0 };
    } catch (error) {
      console.error('Error fetching CashDrawer:', error);
      throw error;
    }
  },
  async openCashDrawer() {
    try {
      return await HprtPrinter.openCashDrawer(0);
    } catch (error) {
      console.error('Error open CashDrawer:', error);
      throw error;
    }
  },
  async getPrinterSN() {
    try {
      return await HprtPrinter.getPrinterSN();
    } catch (error) {
      throw error;
    }
  },
  async getPrintStatus() {
    try {
      const [resPrinter, resPaper, resThePrinter] = await Promise.all([
        HprtPrinter.getPrintStatus(2),
        HprtPrinter.getPrintStatus(4),
        HprtPrinter.getPrintStatus(1),
      ]);
      console.log(resThePrinter);

      const statusPrinter: PrinterStatus = {
        isOpen: (resPrinter | 4) === 4,
        hasPaper: (resPaper | 96) !== 96,
        nearEndPaper: (resPaper | 12) === 12,
      };
      return statusPrinter;
    } catch (error) {
      throw error;
    }
  },
  listenEvent(
    callBackHandle: (payload: { eventName: string; eventData: any }) => void
  ) {
    const eventEmitter = new NativeEventEmitter(
      NativeModules.HprtPrinterModule
    );
    const eventListener = eventEmitter.addListener(
      'eventEmiter',
      (payload: { eventName: string; eventData: any }) => {
        callBackHandle(payload);
      }
    );
    return () => {
      eventListener.remove();
    };
  },
};
export default PrinterHprt;
