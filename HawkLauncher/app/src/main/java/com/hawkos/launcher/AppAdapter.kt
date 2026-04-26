override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
    val tv = (convertView as? TextView) ?: TextView(context)
    val app = apps[position]
    
    // The Niagara Look: Pure text, high-contrast, side-aligned
    tv.text = "> ${app.label.uppercase()}"
    tv.setTextColor(Color.parseColor("#00FF9F"))
    tv.typeface = Typeface.MONOSPACE
    tv.textSize = 14f
    
    // This padding allows for the "Left-Swipe" feel without icons getting in the way
    tv.setPadding(64, 24, 32, 24) 
    tv.setBackgroundColor(Color.TRANSPARENT)
    
    return tv
}
