override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
    val tv = (convertView as? TextView) ?: TextView(context)
    val app = apps[position]
    
    // Formatting: "> APP NAME" with monospace green
    tv.text = "> ${app.label.uppercase()}"
    tv.setTextColor(Color.parseColor("#00FF9F"))
    tv.typeface = Typeface.MONOSPACE
    tv.textSize = 15f
    
    // Add extra padding to the left to simulate a "gutter" for swiping
    tv.setPadding(80, 20, 20, 20) 
    tv.setBackgroundColor(Color.TRANSPARENT)
    
    return tv
}
