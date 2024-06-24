/* eslint-disable no-bitwise */
import { NativeEventEmitter, NativeModules, Platform } from 'react-native';
import {
  PrinterInterface,
  type HprtPrinterInfo,
  type HprtPrinterType,
  type OptionPrinter,
  type PrinterConnection,
  type PrinterImage,
  type PrinterStatus,
  type StatusDrawer,
} from './typing';

export {
  PrinterInterface,
  type HprtPrinterInfo,
  type HprtPrinterType,
  type OptionPrinter,
  type PrinterImage,
  type PrinterStatus,
  type StatusDrawer,
  type PrinterConnection,
};
const arrVenderId = [8401];
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
          if (onPrinterFound && !!printer.serialNumber) {
            onPrinterFound(printer);
          }
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
      let printers =
        interfacePrinter === PrinterInterface.USB
          ? result.filter((item) => arrVenderId.includes(item.vendorId))
          : result;
      totalPrinters = printers.length;
      if (interfacePrinter === PrinterInterface.USB) {
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
            if (processedPrinters >= totalPrinters) {
              onFinished();
              eventListener.remove();
            }
          }
        }
      } else {
        for (const printer of printers) {
          onPrinterFound(printer);
        }
        onFinished();
      }
    } catch (error) {
      console.error('Error fetching devices:', error);
      onFinished();
      throw error;
    }
  },
  async onConnect(printer: PrinterConnection) {
    if (!printer) {
      throw 'Not found printer';
    }
    try {
      const printerRequest = { ...printer };
      if (printer.interface === PrinterInterface.LAN) {
        const result: HprtPrinterInfo[] = await NetPrinter.getDevices();
        const printerLan = result.find(
          (item) => item.serialNumber === printerRequest.identifier
        );
        printerRequest.identifier = printerLan?.ipDevice || '';
      }

      return await HprtPrinter.connectDevice(printerRequest);
    } catch (error) {
      throw error;
    }
  },
  async onDisConnect() {
    try {
      return await HprtPrinter.disConnectDevice();
    } catch (error) {
      throw error;
    }
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
      const [resPrinter, resPaper] = await Promise.all([
        HprtPrinter.getPrintStatus(2),
        HprtPrinter.getPrintStatus(4),
      ]);
      if (!resPrinter && !resPaper) {
        return { isConnected: false } as PrinterStatus;
      }

      const statusPrinter: PrinterStatus = {
        isOpen: (resPrinter & 4) === 4,
        hasPaper: (resPaper & 96) !== 96,
        nearEndPaper: (resPaper & 12) === 12,
        isNomal: !((resPrinter & 4) === 4) && (resPaper & 96) !== 96,
        isConnected: true,
      };
      return statusPrinter;
    } catch (error) {
      return { isConnected: false } as PrinterStatus;
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
