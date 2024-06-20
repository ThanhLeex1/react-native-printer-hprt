export interface OptionPrinter {
  isBase64?: boolean;
  size?: number;
  isRotate?: boolean;
  isLzo?: boolean;
}

export interface PrinterImage {
  source: string;
  options?: OptionPrinter;
}

export interface HprtPrinterInfo {
  deviceName: string;
  version: string;
  manufacturerName: string;
  serialNumber: string;
  productName: string;
  vendorId: number;
  productId: number;
  deviceId: number;
  deviceProtocol: number;
  deviceClass: number;
  deviceSubclass: number;
  ipDevice: string;
  macAddress: string;
  status: string;
  interfacePrinter: PrinterInterface;
}
export interface PrinterConnection {
  interface: PrinterInterface;
  identifier: string;
}

export interface HprtPrinterInfo {
  deviceName: string;
  version: string;
  manufacturerName: string;
  serialNumber: string;
  productName: string;
  vendorId: number;
  productId: number;
  deviceId: number;
  deviceProtocol: number;
  deviceClass: number;
  deviceSubclass: number;
  ipDevice: string;
  macAddress: string;
  status: string;
  interfacePrinter: PrinterInterface;
}

export enum PrinterInterface {
  USB = 'USB',
  LAN = 'LAN',
  BLUETOOTH = 'BLUETOOTH',
}
export interface PrinterStatus {
  isOpen?: boolean;
  hasPaper?: boolean;
  nearEndPaper?: boolean;
  isNomal?: boolean;
}

export interface StatusDrawer {
  isOpen: boolean;
}

interface OnDiscoveryFinished {
  (): void;
}

interface OnPrinterFounnd {
  (printer: HprtPrinterInfo): void;
}

export type HprtPrinterType = {
  discoveryDevices: (
    interfacePrinter: PrinterInterface,
    OnPrinterFounnd: OnPrinterFounnd,
    onFinished: OnDiscoveryFinished
  ) => Promise<void>;
  getPrinterSN: () => Promise<string>;
  openCashDrawer: () => Promise<number>;
  getCashDrawer: () => Promise<StatusDrawer>;
  getPrintStatus: () => Promise<PrinterStatus>;
  cutPaper: () => Promise<PrinterStatus>;
  onConnect: (printer: PrinterConnection) => Promise<number>;
  onDisConnect: () => Promise<void>;
  listenEvent: (
    callBackHandle: (payload: { eventName: string; eventData: any }) => void
  ) => () => void;
  printImage: (request: PrinterImage) => Promise<PrinterStatus>;
};
