import PrinterHprt from 'hprt-printer';
import * as React from 'react';

import { StyleSheet, View } from 'react-native';
import { PrinterInterface, type HprtPrinterInfo } from '../../src/typing';

export default function App() {
  const printerRef = React.useRef<HprtPrinterInfo>();
  React.useEffect(() => {
    getDevice();
  }, []);

  const getDevice = async () => {
    try {
      await PrinterHprt.discoveryDevices(
        PrinterInterface.USB,
        (printer) => {
          printerRef.current = printer as HprtPrinterInfo;
        },
        async () => {
          console.log(printerRef.current);
          // const res = await PrinterHprt.onConnect({
          //   identifier: printerRef.current?.serialNumber,
          //   interface: printerRef.current?.interfacePrinter,
          // });
          // console.log(res);
        }
      );
    } catch (error) {
      console.log(error);
    }
  };
  return (
    <View style={styles.container}>{/* <Text>Result: {result}</Text> */}</View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  box: {
    width: 60,
    height: 60,
    marginVertical: 20,
  },
});
