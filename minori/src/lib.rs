extern crate proc_macro;
use proc_macro::TokenStream;
use quote::quote;
use syn::DeriveInput;

#[proc_macro_derive(Minori)]
pub fn minori(input: TokenStream) -> TokenStream{
    //构建ast语法树
    let ast: DeriveInput = syn::parse(input).unwrap();

    // 实现代码
    let code = quote! {
        impl Minori for &ast.ident {
        }
    };
    code.into()
}
