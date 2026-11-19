import '@fontsource/dm-sans/latin-400.css';
import '@fontsource/dm-sans/latin-500.css';
import '@fontsource/dm-sans/latin-600.css';
import '@fontsource/dm-sans/latin-700.css';
import '@fontsource/barlow-condensed/latin-700.css';
import './styles.css';

export const metadata = {
  title: 'Ponto do Lanche | Cardápio',
  description: 'Monte seu pedido, consulte o estoque e finalize pelo balcão digital.',
};

export default function RootLayout({ children }) {
  return <html lang="pt-BR"><body>{children}</body></html>;
}
