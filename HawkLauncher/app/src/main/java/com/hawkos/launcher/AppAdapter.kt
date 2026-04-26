override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
    val tv = (convertView as? TextView) ?: TextView(context)
    val app = apps[position]
    
    // Pure text, no icons
    tv.text = app.label.uppercase()
    tv.setTextColor(Color.parseColor("#00FF9F"))
    tv.textSize = 16f 
    tv.typeface = Typeface.MONOSPACE
    tv.setPadding(40, 20, 10, 20)
    
    return tv
}
