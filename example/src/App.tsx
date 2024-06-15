import PrinterHprt from 'hprt-printer';
import * as React from 'react';

import { StyleSheet, View } from 'react-native';
import { PrinterInterface } from '../../src/typing';

export default function App() {
  React.useEffect(() => {
    getDevice();
  }, []);

  const getDevice = async () => {
    try {
      await PrinterHprt.discoveryDevices(
        PrinterInterface.USB,
        (printer) => {
          console.log(printer);
        },
        () => {
          console.log('Fisnish');
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
