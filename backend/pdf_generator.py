"""
PDF Generator for Inventory Management System
Generates PDF reports for product lists
"""

import io
from datetime import datetime
from typing import List
from reportlab.lib import colors
from reportlab.lib.pagesizes import letter, A4
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.units import inch
from reportlab.platypus import SimpleDocTemplate, Table, TableStyle, Paragraph, Spacer, PageBreak
from reportlab.lib.enums import TA_CENTER, TA_LEFT, TA_RIGHT

from models import Product


class PDFGenerator:
    """Generate PDF reports for inventory data"""
    
    def __init__(self):
        self.styles = getSampleStyleSheet()
        self._setup_custom_styles()
    
    def _setup_custom_styles(self):
        """Setup custom paragraph styles"""
        # Title style
        self.styles.add(ParagraphStyle(
            name='CustomTitle',
            parent=self.styles['Heading1'],
            fontSize=24,
            textColor=colors.HexColor('#667eea'),
            spaceAfter=30,
            alignment=TA_CENTER,
            fontName='Helvetica-Bold'
        ))
        
        # Subtitle style
        self.styles.add(ParagraphStyle(
            name='CustomSubtitle',
            parent=self.styles['Normal'],
            fontSize=12,
            textColor=colors.grey,
            spaceAfter=20,
            alignment=TA_CENTER
        ))
        
        # Header style
        self.styles.add(ParagraphStyle(
            name='CustomHeader',
            parent=self.styles['Heading2'],
            fontSize=14,
            textColor=colors.HexColor('#2c3e50'),
            spaceAfter=12,
            fontName='Helvetica-Bold'
        ))
    
    def generate_full_product_list(self, products: List[Product]) -> bytes:
        """
        Generate PDF for complete product list
        
        Args:
            products: List of all products
            
        Returns:
            PDF file as bytes
        """
        buffer = io.BytesIO()
        doc = SimpleDocTemplate(buffer, pagesize=letter, topMargin=0.5*inch, bottomMargin=0.5*inch)
        story = []
        
        # Title
        title = Paragraph("Complete Product Inventory", self.styles['CustomTitle'])
        story.append(title)
        
        # Subtitle with date and count
        subtitle_text = f"Generated on {datetime.now().strftime('%B %d, %Y at %I:%M %p')}<br/>Total Products: {len(products)}"
        subtitle = Paragraph(subtitle_text, self.styles['CustomSubtitle'])
        story.append(subtitle)
        story.append(Spacer(1, 0.3*inch))
        
        # Summary statistics
        total_quantity = sum(p.quantity for p in products)
        total_value = sum(p.quantity * p.price for p in products)
        low_stock_count = sum(1 for p in products if p.quantity < 10)
        
        summary_data = [
            ['Total Products', 'Total Quantity', 'Total Value', 'Low Stock Items'],
            [str(len(products)), str(total_quantity), f'${total_value:,.2f}', str(low_stock_count)]
        ]
        
        summary_table = Table(summary_data, colWidths=[1.5*inch, 1.5*inch, 1.5*inch, 1.5*inch])
        summary_table.setStyle(TableStyle([
            ('BACKGROUND', (0, 0), (-1, 0), colors.HexColor('#667eea')),
            ('TEXTCOLOR', (0, 0), (-1, 0), colors.whitesmoke),
            ('ALIGN', (0, 0), (-1, -1), 'CENTER'),
            ('FONTNAME', (0, 0), (-1, 0), 'Helvetica-Bold'),
            ('FONTSIZE', (0, 0), (-1, 0), 11),
            ('BOTTOMPADDING', (0, 0), (-1, 0), 12),
            ('BACKGROUND', (0, 1), (-1, -1), colors.HexColor('#f0f4ff')),
            ('TEXTCOLOR', (0, 1), (-1, -1), colors.HexColor('#2c3e50')),
            ('FONTNAME', (0, 1), (-1, -1), 'Helvetica-Bold'),
            ('FONTSIZE', (0, 1), (-1, -1), 12),
            ('GRID', (0, 0), (-1, -1), 1, colors.HexColor('#667eea')),
            ('TOPPADDING', (0, 1), (-1, -1), 10),
            ('BOTTOMPADDING', (0, 1), (-1, -1), 10),
        ]))
        story.append(summary_table)
        story.append(Spacer(1, 0.4*inch))
        
        # Product list header
        header = Paragraph("Product Details", self.styles['CustomHeader'])
        story.append(header)
        story.append(Spacer(1, 0.1*inch))
        
        # Product table
        data = [['ID', 'Name', 'Category', 'Quantity', 'Price', 'Total Value', 'Status']]
        
        for product in products:
            total_product_value = product.quantity * product.price
            status = '⚠️ Low Stock' if product.quantity < 10 else '✓ In Stock'
            
            data.append([
                str(product.id),
                product.name[:30] + '...' if len(product.name) > 30 else product.name,
                product.category,
                str(product.quantity),
                f'${product.price:.2f}',
                f'${total_product_value:.2f}',
                status
            ])
        
        # Create table with appropriate column widths
        col_widths = [0.4*inch, 2*inch, 1.2*inch, 0.8*inch, 0.8*inch, 1*inch, 1*inch]
        table = Table(data, colWidths=col_widths, repeatRows=1)
        
        # Table styling
        table_style = TableStyle([
            # Header row
            ('BACKGROUND', (0, 0), (-1, 0), colors.HexColor('#667eea')),
            ('TEXTCOLOR', (0, 0), (-1, 0), colors.whitesmoke),
            ('ALIGN', (0, 0), (-1, 0), 'CENTER'),
            ('FONTNAME', (0, 0), (-1, 0), 'Helvetica-Bold'),
            ('FONTSIZE', (0, 0), (-1, 0), 10),
            ('BOTTOMPADDING', (0, 0), (-1, 0), 10),
            ('TOPPADDING', (0, 0), (-1, 0), 10),
            
            # Data rows
            ('BACKGROUND', (0, 1), (-1, -1), colors.white),
            ('TEXTCOLOR', (0, 1), (-1, -1), colors.HexColor('#2c3e50')),
            ('ALIGN', (0, 1), (0, -1), 'CENTER'),  # ID column
            ('ALIGN', (1, 1), (2, -1), 'LEFT'),    # Name and Category
            ('ALIGN', (3, 1), (-1, -1), 'CENTER'), # Numbers and status
            ('FONTNAME', (0, 1), (-1, -1), 'Helvetica'),
            ('FONTSIZE', (0, 1), (-1, -1), 9),
            ('TOPPADDING', (0, 1), (-1, -1), 6),
            ('BOTTOMPADDING', (0, 1), (-1, -1), 6),
            
            # Grid
            ('GRID', (0, 0), (-1, -1), 0.5, colors.grey),
            
            # Alternating row colors
            ('ROWBACKGROUNDS', (0, 1), (-1, -1), [colors.white, colors.HexColor('#f8f9ff')]),
        ])
        
        # Highlight low stock items
        for i, product in enumerate(products, start=1):
            if product.quantity < 10:
                table_style.add('TEXTCOLOR', (6, i), (6, i), colors.HexColor('#ff9800'))
                table_style.add('FONTNAME', (6, i), (6, i), 'Helvetica-Bold')
        
        table.setStyle(table_style)
        story.append(table)
        
        # Footer
        story.append(Spacer(1, 0.3*inch))
        footer_text = f"<i>Report generated by Inventory Management System</i>"
        footer = Paragraph(footer_text, self.styles['CustomSubtitle'])
        story.append(footer)
        
        # Build PDF
        doc.build(story)
        
        # Get PDF bytes
        pdf_bytes = buffer.getvalue()
        buffer.close()
        
        return pdf_bytes
    
    def generate_low_stock_report(self, products: List[Product]) -> bytes:
        """
        Generate PDF for low stock products (quantity < 10)
        
        Args:
            products: List of all products
            
        Returns:
            PDF file as bytes
        """
        # Filter low stock products
        low_stock_products = [p for p in products if p.quantity < 10]
        
        buffer = io.BytesIO()
        doc = SimpleDocTemplate(buffer, pagesize=letter, topMargin=0.5*inch, bottomMargin=0.5*inch)
        story = []
        
        # Title
        title = Paragraph("⚠️ Low Stock Alert Report", self.styles['CustomTitle'])
        story.append(title)
        
        # Subtitle
        subtitle_text = f"Generated on {datetime.now().strftime('%B %d, %Y at %I:%M %p')}<br/>Low Stock Items: {len(low_stock_products)}"
        subtitle = Paragraph(subtitle_text, self.styles['CustomSubtitle'])
        story.append(subtitle)
        story.append(Spacer(1, 0.3*inch))
        
        if not low_stock_products:
            # No low stock items
            no_items_text = "<para align=center><b>✓ All products are adequately stocked!</b><br/><br/>No items with quantity below 10.</para>"
            no_items = Paragraph(no_items_text, self.styles['Normal'])
            story.append(no_items)
        else:
            # Summary
            total_low_stock_value = sum(p.quantity * p.price for p in low_stock_products)
            critical_items = sum(1 for p in low_stock_products if p.quantity < 5)
            
            summary_data = [
                ['Low Stock Items', 'Critical Items (< 5)', 'Total Value at Risk'],
                [str(len(low_stock_products)), str(critical_items), f'${total_low_stock_value:,.2f}']
            ]
            
            summary_table = Table(summary_data, colWidths=[2*inch, 2*inch, 2*inch])
            summary_table.setStyle(TableStyle([
                ('BACKGROUND', (0, 0), (-1, 0), colors.HexColor('#ff9800')),
                ('TEXTCOLOR', (0, 0), (-1, 0), colors.whitesmoke),
                ('ALIGN', (0, 0), (-1, -1), 'CENTER'),
                ('FONTNAME', (0, 0), (-1, 0), 'Helvetica-Bold'),
                ('FONTSIZE', (0, 0), (-1, 0), 11),
                ('BOTTOMPADDING', (0, 0), (-1, 0), 12),
                ('BACKGROUND', (0, 1), (-1, -1), colors.HexColor('#fff5f5')),
                ('TEXTCOLOR', (0, 1), (-1, -1), colors.HexColor('#ff9800')),
                ('FONTNAME', (0, 1), (-1, -1), 'Helvetica-Bold'),
                ('FONTSIZE', (0, 1), (-1, -1), 14),
                ('GRID', (0, 0), (-1, -1), 1, colors.HexColor('#ff9800')),
                ('TOPPADDING', (0, 1), (-1, -1), 10),
                ('BOTTOMPADDING', (0, 1), (-1, -1), 10),
            ]))
            story.append(summary_table)
            story.append(Spacer(1, 0.4*inch))
            
            # Product list header
            header = Paragraph("Products Requiring Attention", self.styles['CustomHeader'])
            story.append(header)
            story.append(Spacer(1, 0.1*inch))
            
            # Sort by quantity (lowest first)
            low_stock_products.sort(key=lambda p: p.quantity)
            
            # Product table
            data = [['Priority', 'ID', 'Name', 'Category', 'Qty', 'Price', 'Total Value', 'Reorder']]
            
            for product in low_stock_products:
                total_product_value = product.quantity * product.price
                priority = '🔴 Critical' if product.quantity < 5 else '🟡 Low'
                suggested_reorder = max(20 - product.quantity, 10)  # Suggest reordering to 20 units
                
                data.append([
                    priority,
                    str(product.id),
                    product.name[:25] + '...' if len(product.name) > 25 else product.name,
                    product.category,
                    str(product.quantity),
                    f'${product.price:.2f}',
                    f'${total_product_value:.2f}',
                    f'+{suggested_reorder}'
                ])
            
            # Create table
            col_widths = [0.9*inch, 0.4*inch, 1.8*inch, 1.1*inch, 0.5*inch, 0.7*inch, 0.9*inch, 0.7*inch]
            table = Table(data, colWidths=col_widths, repeatRows=1)
            
            # Table styling
            table_style = TableStyle([
                # Header row
                ('BACKGROUND', (0, 0), (-1, 0), colors.HexColor('#ff9800')),
                ('TEXTCOLOR', (0, 0), (-1, 0), colors.whitesmoke),
                ('ALIGN', (0, 0), (-1, 0), 'CENTER'),
                ('FONTNAME', (0, 0), (-1, 0), 'Helvetica-Bold'),
                ('FONTSIZE', (0, 0), (-1, 0), 9),
                ('BOTTOMPADDING', (0, 0), (-1, 0), 10),
                ('TOPPADDING', (0, 0), (-1, 0), 10),
                
                # Data rows
                ('BACKGROUND', (0, 1), (-1, -1), colors.white),
                ('TEXTCOLOR', (0, 1), (-1, -1), colors.HexColor('#2c3e50')),
                ('ALIGN', (0, 1), (1, -1), 'CENTER'),  # Priority and ID
                ('ALIGN', (2, 1), (3, -1), 'LEFT'),    # Name and Category
                ('ALIGN', (4, 1), (-1, -1), 'CENTER'), # Numbers
                ('FONTNAME', (0, 1), (-1, -1), 'Helvetica'),
                ('FONTSIZE', (0, 1), (-1, -1), 9),
                ('TOPPADDING', (0, 1), (-1, -1), 6),
                ('BOTTOMPADDING', (0, 1), (-1, -1), 6),
                
                # Grid
                ('GRID', (0, 0), (-1, -1), 0.5, colors.grey),
                
                # Alternating row colors
                ('ROWBACKGROUNDS', (0, 1), (-1, -1), [colors.HexColor('#fff5f5'), colors.HexColor('#ffe8e8')]),
            ])
            
            # Highlight critical items
            for i, product in enumerate(low_stock_products, start=1):
                if product.quantity < 5:
                    table_style.add('TEXTCOLOR', (0, i), (0, i), colors.HexColor('#dc2626'))
                    table_style.add('FONTNAME', (0, i), (0, i), 'Helvetica-Bold')
            
            table.setStyle(table_style)
            story.append(table)
            
            # Recommendations
            story.append(Spacer(1, 0.3*inch))
            recommendations_header = Paragraph("📋 Recommendations", self.styles['CustomHeader'])
            story.append(recommendations_header)
            
            recommendations_text = """
            • <b>Critical items (&lt; 5 units):</b> Immediate reorder required<br/>
            • <b>Low stock items (&lt; 10 units):</b> Schedule reorder within 1-2 weeks<br/>
            • <b>Suggested reorder quantities:</b> Shown in "Reorder" column<br/>
            • <b>Review pricing:</b> Consider bulk purchase discounts for frequently low-stock items
            """
            recommendations = Paragraph(recommendations_text, self.styles['Normal'])
            story.append(recommendations)
        
        # Footer
        story.append(Spacer(1, 0.3*inch))
        footer_text = f"<i>Report generated by Inventory Management System</i>"
        footer = Paragraph(footer_text, self.styles['CustomSubtitle'])
        story.append(footer)
        
        # Build PDF
        doc.build(story)
        
        # Get PDF bytes
        pdf_bytes = buffer.getvalue()
        buffer.close()
        
        return pdf_bytes
