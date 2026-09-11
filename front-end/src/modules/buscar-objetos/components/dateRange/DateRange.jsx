import { useState } from 'react';
import DatePicker from 'react-datepicker'; 

import 'react-datepicker/dist/react-datepicker.css'; 
import './dateRange.css';

function DateRange({placeholder, arrow, close, change}){
    const [data, setData] = useState([null, null])
    const [calendarVisible, setCalendarVisible] = useState(false)
    
    const [dataInicial, dataFinal] = data; 

    return (
        <div className="calendar">
            <DatePicker className="input__filter date" selectsRange startDate={dataInicial} endDate={dataFinal} 
            placeholderText={placeholder} dateFormat={'dd/MM/yyyy'} onChange={(date) => {setData(date), change('DATA', `${date}`)}}
            minDate={new Date('01/01/2026')} onCalendarOpen ={() => {setCalendarVisible(true)}} 
            onCalendarClose={() => {setCalendarVisible(false)} } />

            {!calendarVisible ? <img className='arrow' src={arrow} alt="" /> : <img className='arrow up' src={arrow} alt="" /> }
            <img className='x' onClick={() => {setData([null, null]), change('DATA', '')}} src={close} alt="" />
        </div>
    ) 
}

export default DateRange